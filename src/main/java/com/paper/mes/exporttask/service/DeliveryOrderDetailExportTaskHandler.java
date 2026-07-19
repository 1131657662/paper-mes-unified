package com.paper.mes.exporttask.service;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.service.DeliveryExportService;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.exporttask.entity.ExportTask;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class DeliveryOrderDetailExportTaskHandler implements ExportTaskHandler {
    public static final String TASK_TYPE = "DELIVERY_ORDER_DETAIL";
    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final DeliveryService deliveryService;
    private final DeliveryExportService exportService;

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
        DeliveryDetailVO detail = deliveryService.getDetail(task.getSourceUuid());
        try (Workbook workbook = exportService.buildWorkbook(detail);
             OutputStream output = Files.newOutputStream(target)) {
            workbook.write(output);
        }
        String filename = "出库单_" + detail.getOrder().getDeliveryNo() + ".xlsx";
        return new ExportTaskArtifact(filename, CONTENT_TYPE);
    }
}
