package com.paper.mes.exporttask.service;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.delivery.service.DeliveryInventoryExportService;
import com.paper.mes.exporttask.entity.ExportTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DeliveryInventoryExportTaskHandler implements ExportTaskHandler {
    public static final String TASK_TYPE = "DELIVERY_INVENTORY";
    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final DeliveryInventoryExportService exportService;

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public String requiredPermission() {
        return Permissions.DELIVERY_VIEW;
    }

    @Override
    public String fileExtension() {
        return "xlsx";
    }

    @Override
    public ExportTaskArtifact generate(ExportTask task, Path target) throws Exception {
        exportService.exportSnapshotToPath(task.getQuerySnapshotUuid(), target);
        return new ExportTaskArtifact("成品库存_" + LocalDate.now() + ".xlsx", CONTENT_TYPE);
    }
}
