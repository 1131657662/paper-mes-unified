package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.paper.mes.exporttask.dto.ReportExportTaskPayload;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.service.ReportQuerySnapshotBundle;
import com.paper.mes.report.service.ReportQuerySnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExportTaskCreationService {
    private final ExportTaskMapper taskMapper;
    private final ExportTaskExecutor taskExecutor;
    private final ExportTaskLifecycleService lifecycleService;
    private final ExportTaskDocumentResolver documentResolver;
    private final PermissionChecker permissionChecker;
    private final ExportTaskPayloadWriter payloadWriter;
    private final ExportTaskStorage storage;
    private final ReportQuerySnapshotService reportQuerySnapshotService;
    private final DeliveryOrderExportRevisionSnapshot deliveryOrderRevisionSnapshot;
    private final ProcessOrderExportRevisionSnapshot processOrderRevisionSnapshot;
    private final DeliveryExportSnapshotTaskCreator deliveryExportSnapshotTaskCreator;

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
        ProcessOrder order = documentResolver.processOrder(orderUuid);
        String payload = processOrderRevisionSnapshot.capture(orderUuid, dto.getCustomerRevisionNo());
        ExportTask existing = findByRequest(user.getUuid(), dto.getRequestId());
        if (existing != null) {
            return requireSameDocumentSnapshot(
                    existing, ProcessOrderDetailExportTaskHandler.TASK_TYPE, orderUuid, payload);
        }
        ExportTask task = ExportTaskFactory.processOrder(dto.getRequestId().trim(), user, order, payload);
        return insertAndSubmit(task, () -> requireSameDocumentSnapshot(
                findByRequest(user.getUuid(), dto.getRequestId()),
                ProcessOrderDetailExportTaskHandler.TASK_TYPE, orderUuid, payload));
    }

    public String createDeliveryOrderTask(String orderUuid, ExportTaskCreateDTO dto) {
        permissionChecker.require(Permissions.DELIVERY_VIEW);
        CurrentUser user = currentUser();
        DeliveryOrder order = documentResolver.deliveryOrder(orderUuid);
        String payload = deliveryOrderRevisionSnapshot.capture(
                orderUuid, dto.getCustomerRevisionNo(), order.getDeliveryStatus());
        ExportTask existing = findByRequest(user.getUuid(), dto.getRequestId());
        if (existing != null) {
            return requireSameDocumentSnapshot(
                    existing, DeliveryOrderDetailExportTaskHandler.TASK_TYPE, orderUuid, payload);
        }
        ExportTask task = ExportTaskFactory.deliveryOrder(dto.getRequestId().trim(), user, order, payload);
        return insertAndSubmit(task, () -> requireSameDocumentSnapshot(
                findByRequest(user.getUuid(), dto.getRequestId()),
                DeliveryOrderDetailExportTaskHandler.TASK_TYPE, orderUuid, payload));
    }

    public String createDeliveryInventoryTask(DeliveryInventoryExportTaskCreateDTO dto) {
        permissionChecker.require(Permissions.DELIVERY_VIEW);
        CurrentUser user = currentUser();
        String requestId = dto.getRequestId().trim();
        String payload = payloadWriter.write(dto.getQuery());
        return deliveryExportSnapshotTaskCreator.createInventory(requestId, payload, user, dto.getQuery());
    }

    public String createDeliveryListTask(DeliveryListExportTaskCreateDTO dto) {
        permissionChecker.require(Permissions.DELIVERY_VIEW);
        CurrentUser user = currentUser();
        String requestId = dto.getRequestId().trim();
        String payload = payloadWriter.write(dto.getQuery());
        return deliveryExportSnapshotTaskCreator.createReconciliation(requestId, payload, user, dto.getQuery());
    }

    public String createReportTask(ReportExportTaskCreateDTO dto) {
        permissionChecker.require(Permissions.REPORT_VIEW);
        CurrentUser user = currentUser();
        String requestId = dto.getRequestId().trim();
        ReportQuerySnapshotBundle snapshot = reportQuerySnapshotService.createForExport(dto.getQuery(), user);
        String payload = reportPayload(snapshot, dto.getReportPath());
        return createSnapshotTask(requestId, payload, user,
                ExportTaskFactory.report(requestId, payload, user, dto.getReportPath(),
                        snapshot.snapshot().querySnapshotUuid(), snapshot.snapshot().metricReleaseUuid()),
                ReportExportTaskHandler.TASK_TYPE);
    }

    public String createScheduledReportTask(String requestId, String subscriptionUuid,
                                            String subscriptionName, String reportPath, ReportQuery query,
                                            CurrentUser recipient) {
        if (!permissionChecker.hasRolePermission(recipient.getRoleCode(), Permissions.REPORT_VIEW)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "接收人没有报表权限");
        }
        ReportQuerySnapshotBundle snapshot = reportQuerySnapshotService.createForExport(query, recipient);
        String payload = reportPayload(snapshot, reportPath);
        ExportTask existing = findByRequest(recipient.getUuid(), requestId);
        if (existing != null) {
            String taskUuid = requireSameSnapshot(existing, ReportExportTaskHandler.TASK_TYPE, payload);
            lifecycleService.retryScheduled(existing, recipient);
            return taskUuid;
        }
        ExportTask task = ExportTaskFactory.scheduledReport(requestId, payload, recipient,
                subscriptionUuid, subscriptionName, reportPath, snapshot.snapshot().querySnapshotUuid(),
                snapshot.snapshot().metricReleaseUuid());
        return createSnapshotTask(requestId, payload, recipient, task, ReportExportTaskHandler.TASK_TYPE);
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

    private String requireSameDocumentSnapshot(
            ExportTask task, String taskType, String sourceUuid, String payload) {
        if (task == null || !taskType.equals(task.getTaskType())
                || !sourceUuid.equals(task.getSourceUuid())
                || !payload.equals(task.getRequestPayload())) {
            throw new BusinessException("请求号已用于其他导出任务");
        }
        return task.getUuid();
    }

    private String reportPayload(ReportQuerySnapshotBundle snapshot, String reportPath) {
        return payloadWriter.write(new ReportExportTaskPayload(ReportExportTaskPayload.CURRENT_SCHEMA_VERSION,
                snapshot.snapshot().querySnapshotUuid(), reportPath, snapshot.query(), snapshot.snapshot()));
    }

    private CurrentUser currentUser() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效");
        }
        return user;
    }
}
