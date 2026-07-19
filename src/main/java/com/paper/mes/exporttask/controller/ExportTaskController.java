package com.paper.mes.exporttask.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.exporttask.dto.ExportTaskAcknowledgeDTO;
import com.paper.mes.exporttask.dto.ExportTaskCreateDTO;
import com.paper.mes.exporttask.dto.DeliveryInventoryExportTaskCreateDTO;
import com.paper.mes.exporttask.dto.DeliveryListExportTaskCreateDTO;
import com.paper.mes.exporttask.dto.ExportTaskSummaryVO;
import com.paper.mes.exporttask.dto.ExportTaskHistoryQuery;
import com.paper.mes.exporttask.dto.ExportTaskHistoryVO;
import com.paper.mes.exporttask.dto.ReportExportTaskCreateDTO;
import com.paper.mes.exporttask.service.ExportTaskService;
import com.paper.mes.exporttask.service.ExportTaskCreationService;
import com.paper.mes.exporttask.service.ExportTaskLifecycleService;
import com.paper.mes.exporttask.service.ExportTaskHistoryService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export-tasks")
@RequiredArgsConstructor
public class ExportTaskController {
    private final ExportTaskService exportTaskService;
    private final ExportTaskCreationService creationService;
    private final ExportTaskLifecycleService lifecycleService;
    private final ExportTaskHistoryService historyService;

    @GetMapping
    @RequirePermission(Permissions.EXPORT_TASK_VIEW)
    public R<ExportTaskSummaryVO> summary() {
        return R.success(exportTaskService.summary());
    }

    @GetMapping("/history")
    @RequirePermission(Permissions.EXPORT_TASK_VIEW)
    public R<ExportTaskHistoryVO> history(@Valid ExportTaskHistoryQuery query) {
        return R.success(historyService.page(query));
    }

    @PostMapping("/settle-orders/{uuid}")
    @RequirePermission(Permissions.SETTLE_VIEW)
    public R<String> createSettle(@PathVariable String uuid, @Valid @RequestBody ExportTaskCreateDTO dto) {
        return R.success(creationService.createSettleTask(uuid, dto));
    }

    @PostMapping("/process-orders/{uuid}")
    @RequirePermission(Permissions.ORDER_VIEW)
    public R<String> createProcessOrder(@PathVariable String uuid, @Valid @RequestBody ExportTaskCreateDTO dto) {
        return R.success(creationService.createProcessOrderTask(uuid, dto));
    }

    @PostMapping("/delivery-orders/{uuid}")
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<String> createDeliveryOrder(@PathVariable String uuid, @Valid @RequestBody ExportTaskCreateDTO dto) {
        return R.success(creationService.createDeliveryOrderTask(uuid, dto));
    }

    @PostMapping("/delivery-inventory")
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<String> createDeliveryInventory(@Valid @RequestBody DeliveryInventoryExportTaskCreateDTO dto) {
        return R.success(creationService.createDeliveryInventoryTask(dto));
    }

    @PostMapping("/delivery-reconciliation")
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<String> createDeliveryReconciliation(@Valid @RequestBody DeliveryListExportTaskCreateDTO dto) {
        return R.success(creationService.createDeliveryListTask(dto));
    }

    @PostMapping("/reports")
    @RequirePermission(Permissions.REPORT_VIEW)
    public R<String> createReport(@Valid @RequestBody ReportExportTaskCreateDTO dto) {
        return R.success(creationService.createReportTask(dto));
    }

    @PutMapping("/acknowledge")
    @RequirePermission(Permissions.EXPORT_TASK_VIEW)
    public R<Integer> acknowledge(@Valid @RequestBody ExportTaskAcknowledgeDTO dto) {
        return R.success(exportTaskService.acknowledge(dto));
    }

    @GetMapping("/{uuid}/download")
    @RequirePermission(Permissions.EXPORT_TASK_VIEW)
    public void download(@PathVariable String uuid, HttpServletResponse response) {
        exportTaskService.download(uuid, response);
    }

    @PostMapping("/{uuid}/retry")
    @RequirePermission(Permissions.EXPORT_TASK_VIEW)
    public R<Void> retry(@PathVariable String uuid) {
        lifecycleService.retry(uuid);
        return R.success();
    }

    @PostMapping("/{uuid}/cancel")
    @RequirePermission(Permissions.EXPORT_TASK_VIEW)
    public R<Void> cancel(@PathVariable String uuid) {
        lifecycleService.cancel(uuid);
        return R.success();
    }

    @PutMapping("/{uuid}/acknowledge")
    @RequirePermission(Permissions.EXPORT_TASK_VIEW)
    public R<Void> acknowledgeOne(@PathVariable String uuid) {
        lifecycleService.acknowledge(uuid);
        return R.success();
    }
}
