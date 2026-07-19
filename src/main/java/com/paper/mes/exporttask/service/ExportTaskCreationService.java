package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ResultCode;
import com.paper.mes.exporttask.dto.DeliveryInventoryExportTaskCreateDTO;
import com.paper.mes.exporttask.dto.DeliveryListExportTaskCreateDTO;
import com.paper.mes.exporttask.dto.ExportTaskCreateDTO;
import com.paper.mes.exporttask.dto.ReportExportTaskCreateDTO;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.settle.entity.SettleOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExportTaskCreationService {
    private final ExportTaskMapper taskMapper;
    private final ExportTaskExecutor taskExecutor;
    private final ExportTaskDocumentResolver documentResolver;
    private final PermissionChecker permissionChecker;
    private final ObjectMapper objectMapper;
    private final ExportTaskStorage storage;

    public String createSettleTask(String settleUuid, ExportTaskCreateDTO dto) {
        permissionChecker.require(Permissions.SETTLE_VIEW);
        CurrentUser user = currentUser();
        ExportTask existing = findByRequest(user.getUuid(), dto.getRequestId());
        if (existing != null) {
            return requireSameDocumentRequest(existing, SettleDetailExportTaskHandler.TASK_TYPE, settleUuid);
        }
        SettleOrder order = documentResolver.settle(settleUuid);
        ExportTask task = ExportTaskFactory.settle(dto.getRequestId().trim(), user, order);
        return insertAndSubmit(task, () -> requireSameDocumentRequest(
                findByRequest(user.getUuid(), dto.getRequestId()),
                SettleDetailExportTaskHandler.TASK_TYPE, settleUuid));
    }

    public String createProcessOrderTask(String orderUuid, ExportTaskCreateDTO dto) {
        permissionChecker.require(Permissions.ORDER_VIEW);
        CurrentUser user = currentUser();
        ExportTask existing = findByRequest(user.getUuid(), dto.getRequestId());
        if (existing != null) {
            return requireSameDocumentRequest(existing, ProcessOrderDetailExportTaskHandler.TASK_TYPE, orderUuid);
        }
        ProcessOrder order = documentResolver.processOrder(orderUuid);
        ExportTask task = ExportTaskFactory.processOrder(dto.getRequestId().trim(), user, order);
        return insertAndSubmit(task, () -> requireSameDocumentRequest(
                findByRequest(user.getUuid(), dto.getRequestId()),
                ProcessOrderDetailExportTaskHandler.TASK_TYPE, orderUuid));
    }

    public String createDeliveryOrderTask(String orderUuid, ExportTaskCreateDTO dto) {
        permissionChecker.require(Permissions.DELIVERY_VIEW);
        CurrentUser user = currentUser();
        ExportTask existing = findByRequest(user.getUuid(), dto.getRequestId());
        if (existing != null) {
            return requireSameDocumentRequest(existing, DeliveryOrderDetailExportTaskHandler.TASK_TYPE, orderUuid);
        }
        DeliveryOrder order = documentResolver.deliveryOrder(orderUuid);
        ExportTask task = ExportTaskFactory.deliveryOrder(dto.getRequestId().trim(), user, order);
        return insertAndSubmit(task, () -> requireSameDocumentRequest(
                findByRequest(user.getUuid(), dto.getRequestId()),
                DeliveryOrderDetailExportTaskHandler.TASK_TYPE, orderUuid));
    }

    public String createDeliveryInventoryTask(DeliveryInventoryExportTaskCreateDTO dto) {
        permissionChecker.require(Permissions.DELIVERY_VIEW);
        CurrentUser user = currentUser();
        String requestId = dto.getRequestId().trim();
        String payload = serialize(dto.getQuery());
        return createSnapshotTask(requestId, payload, user,
                ExportTaskFactory.deliveryInventory(requestId, payload, user),
                DeliveryInventoryExportTaskHandler.TASK_TYPE);
    }

    public String createDeliveryListTask(DeliveryListExportTaskCreateDTO dto) {
        permissionChecker.require(Permissions.DELIVERY_VIEW);
        CurrentUser user = currentUser();
        String requestId = dto.getRequestId().trim();
        String payload = serialize(dto.getQuery());
        return createSnapshotTask(requestId, payload, user,
                ExportTaskFactory.deliveryList(requestId, payload, user),
                DeliveryListExportTaskHandler.TASK_TYPE);
    }

    public String createReportTask(ReportExportTaskCreateDTO dto) {
        permissionChecker.require(Permissions.REPORT_VIEW);
        CurrentUser user = currentUser();
        String requestId = dto.getRequestId().trim();
        String payload = serialize(dto.getQuery());
        return createSnapshotTask(requestId, payload, user,
                ExportTaskFactory.report(requestId, payload, user),
                ReportExportTaskHandler.TASK_TYPE);
    }

    private String createSnapshotTask(String requestId, String payload, CurrentUser user,
                                      ExportTask task, String taskType) {
        ExportTask existing = findByRequest(user.getUuid(), requestId);
        if (existing != null) return requireSameSnapshot(existing, taskType, payload);
        return insertAndSubmit(task, () -> requireSameSnapshot(
                findByRequest(user.getUuid(), requestId), taskType, payload));
    }

    private String insertAndSubmit(ExportTask task, java.util.function.Supplier<String> duplicateResult) {
        storage.assertReadyForWrite();
        try {
            ConcurrencyGuard.requireRowUpdated(taskMapper.insert(task));
        } catch (DuplicateKeyException exception) {
            return duplicateResult.get();
        }
        taskExecutor.submit(task.getUuid());
        return task.getUuid();
    }

    private ExportTask findByRequest(String userUuid, String requestId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<ExportTask>()
                .eq(ExportTask::getRequesterUuid, userUuid)
                .eq(ExportTask::getRequestId, requestId.trim()));
    }

    private String requireSameSnapshot(ExportTask task, String taskType, String payload) {
        if (task == null || !taskType.equals(task.getTaskType()) || !payload.equals(task.getRequestPayload())) {
            throw new BusinessException("请求号已用于其他导出任务");
        }
        return task.getUuid();
    }

    private String requireSameDocumentRequest(ExportTask task, String taskType, String sourceUuid) {
        if (task == null || !taskType.equals(task.getTaskType())
                || !sourceUuid.equals(task.getSourceUuid())) {
            throw new BusinessException("请求号已用于其他导出任务");
        }
        return task.getUuid();
    }

    private String serialize(Object query) {
        try {
            return objectMapper.writeValueAsString(query);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("导出筛选条件无法保存");
        }
    }

    private CurrentUser currentUser() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效");
        }
        return user;
    }
}
