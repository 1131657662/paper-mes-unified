package com.paper.mes.exporttask.service;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.service.ProcessOrderExportService;
import com.paper.mes.processorder.service.ProcessOrderService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class ProcessOrderDetailExportTaskHandler implements ExportTaskHandler {
    public static final String TASK_TYPE = "PROCESS_ORDER_DETAIL";
    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ProcessOrderService processOrderService;
    private final ProcessOrderExportService exportService;
    private final ProcessOrderExportRevisionSnapshot revisionSnapshot;

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public String requiredPermission() {
        return Permissions.ORDER_VIEW;
    }

    @Override
    public String fileExtension() {
        return "xlsx";
    }

    @Override
    public ExportTaskArtifact generate(ExportTask task, Path target) throws Exception {
        revisionSnapshot.verifyCurrent(task.getSourceUuid(), task.getRequestPayload());
        ProcessOrderDetailVO detail = processOrderService.getDetail(task.getSourceUuid());
        try (Workbook workbook = exportService.buildWorkbook(detail);
             OutputStream output = Files.newOutputStream(target)) {
            workbook.write(output);
        }
        revisionSnapshot.verifyCurrent(task.getSourceUuid(), task.getRequestPayload());
        String filename = "加工单资料_" + detail.getOrder().getOrderNo() + ".xlsx";
        return new ExportTaskArtifact(filename, CONTENT_TYPE);
    }
}
