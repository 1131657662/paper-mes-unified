package com.paper.mes.exporttask.service;

import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.settle.entity.SettleOrder;

import java.time.LocalDateTime;

final class ExportTaskFactory {
    private static final int STATUS_QUEUED = 1;

    private ExportTaskFactory() {
    }

    static ExportTask settle(String requestId, CurrentUser user, SettleOrder order) {
        ExportTask task = base(requestId, user);
        task.setTaskType(SettleDetailExportTaskHandler.TASK_TYPE);
        task.setModuleCode("settle");
        task.setOperationCode("detail-export");
        task.setTaskName("结算单-" + order.getSettleNo());
        task.setSourceUuid(order.getUuid());
        return task;
    }

    static ExportTask processOrder(String requestId, CurrentUser user, ProcessOrder order) {
        ExportTask task = base(requestId, user);
        task.setTaskType(ProcessOrderDetailExportTaskHandler.TASK_TYPE);
        task.setModuleCode("process-order");
        task.setOperationCode("detail-export");
        task.setTaskName("加工单 " + order.getOrderNo());
        task.setSourceUuid(order.getUuid());
        return task;
    }

    static ExportTask deliveryOrder(String requestId, CurrentUser user, DeliveryOrder order) {
        ExportTask task = base(requestId, user);
        task.setTaskType(DeliveryOrderDetailExportTaskHandler.TASK_TYPE);
        task.setModuleCode("delivery");
        task.setOperationCode("detail-export");
        task.setTaskName("出库单 " + order.getDeliveryNo());
        task.setSourceUuid(order.getUuid());
        return task;
    }

    static ExportTask deliveryInventory(String requestId, String payload, CurrentUser user) {
        ExportTask task = base(requestId, user);
        task.setTaskType(DeliveryInventoryExportTaskHandler.TASK_TYPE);
        task.setModuleCode("delivery");
        task.setOperationCode("inventory-export");
        task.setTaskName("成品库存导出");
        task.setSourceUuid("delivery-inventory");
        task.setRequestPayload(payload);
        return task;
    }

    static ExportTask deliveryList(String requestId, String payload, CurrentUser user) {
        ExportTask task = base(requestId, user);
        task.setTaskType(DeliveryListExportTaskHandler.TASK_TYPE);
        task.setModuleCode("delivery");
        task.setOperationCode("reconciliation-export");
        task.setTaskName("出库对账导出");
        task.setSourceUuid("delivery-reconciliation");
        task.setRequestPayload(payload);
        return task;
    }

    static ExportTask report(String requestId, String payload, CurrentUser user) {
        ExportTask task = base(requestId, user);
        task.setTaskType(ReportExportTaskHandler.TASK_TYPE);
        task.setModuleCode("report");
        task.setOperationCode("full-export");
        task.setTaskName("统计报表导出");
        task.setSourceUuid("report");
        task.setRequestPayload(payload);
        return task;
    }

    private static ExportTask base(String requestId, CurrentUser user) {
        ExportTask task = new ExportTask();
        task.setRequestId(requestId);
        task.setRequesterUuid(user.getUuid());
        task.setRequesterName(displayName(user));
        task.setTaskStatus(STATUS_QUEUED);
        task.setProgress(0);
        task.setAttemptCount(0);
        task.setMaxAttempts(3);
        task.setDownloadCount(0);
        task.setExpiresAt(LocalDateTime.now().plusDays(7));
        return task;
    }

    private static String displayName(CurrentUser user) {
        return user.getRealName() == null || user.getRealName().isBlank() ? user.getUsername() : user.getRealName();
    }
}
