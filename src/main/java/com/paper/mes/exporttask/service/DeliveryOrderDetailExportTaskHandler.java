package com.paper.mes.exporttask.service;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.delivery.dto.DeliveryCustomerRevisionPreviewVO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.dto.DeliverySortSpec;
import com.paper.mes.delivery.service.DeliveryExportService;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.exporttask.entity.ExportTask;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        List<DeliverySortSpec> physicalSortChain = revisionSnapshot.sortChain(task.getRequestPayload());
        List<DeliverySortSpec> customerSortChain = revisionSnapshot.customerSortChain(task.getRequestPayload());
        List<DeliverySortSpec> traceSortChain = revisionSnapshot.traceSortChain(task.getRequestPayload());
        String documentView = revisionSnapshot.documentView(task.getRequestPayload());
        try (Workbook workbook = workbook(detail, customerSpecs, physicalSortChain, customerSortChain,
                traceSortChain, documentView);
             OutputStream output = Files.newOutputStream(target)) {
            workbook.write(output);
        }
        verifyAfterGeneration(task);
        String filename = "出库单_" + detail.getOrder().getDeliveryNo() + ".xlsx";
        return new ExportTaskArtifact(filename, CONTENT_TYPE);
    }

    private Workbook workbook(DeliveryDetailVO detail, DeliveryCustomerRevisionPreviewVO customerSpecs,
                              List<DeliverySortSpec> physicalSortChain,
                              List<DeliverySortSpec> customerSortChain,
                              List<DeliverySortSpec> traceSortChain,
                              String documentView) {
        boolean hasPhysicalSort = physicalSortChain != null && !physicalSortChain.isEmpty();
        boolean hasCustomerSort = customerSortChain != null && !customerSortChain.isEmpty();
        boolean hasTraceSort = traceSortChain != null && !traceSortChain.isEmpty();
        if (!hasPhysicalSort && !hasCustomerSort && !hasTraceSort) {
            return exportService.buildWorkbook(detail, customerSpecs);
        }
        if (!hasCustomerSort && !hasTraceSort) {
            return exportService.buildWorkbook(detail, customerSpecs, physicalSortChain);
        }
        return exportService.buildWorkbook(detail, customerSpecs, physicalSortChain,
                customerSortChain, traceSortChain, documentView);
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
