package com.paper.mes.exporttask.service;

import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryExportSnapshotTaskCreator {

    private final ExportTaskMapper taskMapper;
    private final ExportTaskExecutor taskExecutor;
    private final ExportTaskStorage storage;
    private final DeliveryExportSnapshotCaptureService snapshotCaptureService;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public String createInventory(String requestId, String payload, CurrentUser user,
                                  DeliveryInventoryFinishQuery query) {
        ExportTask task = ExportTaskFactory.deliveryInventory(requestId, payload, user);
        return create(task, query);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public String createReconciliation(String requestId, String payload, CurrentUser user,
                                       DeliveryQuery query) {
        ExportTask task = ExportTaskFactory.deliveryList(requestId, payload, user);
        return create(task, query);
    }

    private String create(ExportTask task, Object query) {
        task.setQuerySnapshotUuid(UUID.randomUUID().toString());
        storage.assertReadyForWrite();
        try {
            ConcurrencyGuard.requireRowUpdated(taskMapper.insert(task));
        } catch (DuplicateKeyException exception) {
            return requireSame(find(task), task);
        }
        capture(task, query);
        submitAfterCommit(task.getUuid());
        return task.getUuid();
    }

    private void capture(ExportTask task, Object query) {
        if (query instanceof DeliveryInventoryFinishQuery inventoryQuery) {
            snapshotCaptureService.captureInventory(task, inventoryQuery);
            return;
        }
        if (query instanceof DeliveryQuery deliveryQuery) {
            snapshotCaptureService.captureReconciliation(task, deliveryQuery);
            return;
        }
        throw new BusinessException("不支持的出库导出快照类型");
    }

    private ExportTask find(ExportTask requested) {
        return taskMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExportTask>()
                .eq(ExportTask::getRequesterUuid, requested.getRequesterUuid())
                .eq(ExportTask::getRequestId, requested.getRequestId()));
    }

    private String requireSame(ExportTask existing, ExportTask requested) {
        if (existing == null || !requested.getTaskType().equals(existing.getTaskType())
                || !requested.getRequestPayload().equals(existing.getRequestPayload())) {
            throw new BusinessException("请求号已用于其他导出任务");
        }
        return existing.getUuid();
    }

    private void submitAfterCommit(String taskUuid) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            taskExecutor.submit(taskUuid);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                taskExecutor.submit(taskUuid);
            }
        });
    }
}
