package com.paper.mes.exporttask.service;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.delivery.dto.DeliveryCustomerRevisionPreviewVO;
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
    private final DeliveryOrderExportRevisionSnapshot revisionSnapshot;

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
        DeliveryCustomerRevisionPreviewVO customerSpecs = customerSpecs(task, detail);
        try (Workbook workbook = exportService.buildWorkbook(detail, customerSpecs);
             OutputStream output = Files.newOutputStream(target)) {
            workbook.write(output);
        }
        verifyAfterGeneration(task);
        String filename = "出库单_" + detail.getOrder().getDeliveryNo() + ".xlsx";
        return new ExportTaskArtifact(filename, CONTENT_TYPE);
    }

    private DeliveryCustomerRevisionPreviewVO customerSpecs(ExportTask task, DeliveryDetailVO detail) {
        Integer status = detail.getOrder().getDeliveryStatus();
        if (Integer.valueOf(3).equals(status)) {
            revisionSnapshot.verifyVoided(task.getRequestPayload());
            return null;
        }
        return revisionSnapshot.verifyCurrentAndRead(task.getSourceUuid(), task.getRequestPayload());
    }

    private void verifyAfterGeneration(ExportTask task) {
        DeliveryDetailVO current = deliveryService.getDetail(task.getSourceUuid());
        if (Integer.valueOf(3).equals(current.getOrder().getDeliveryStatus())) {
            revisionSnapshot.verifyVoided(task.getRequestPayload());
            return;
        }
        revisionSnapshot.verifyCurrent(task.getSourceUuid(), task.getRequestPayload());
    }
}
