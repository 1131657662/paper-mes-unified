package com.paper.mes.exporttask.service;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.settle.dto.SettleDetailVO;
import com.paper.mes.settle.service.SettleExportService;
import com.paper.mes.settle.service.SettleService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class SettleDetailExportTaskHandler implements ExportTaskHandler {
    public static final String TASK_TYPE = "SETTLE_DETAIL";
    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final SettleService settleService;
    private final SettleExportService settleExportService;

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public String requiredPermission() {
        return Permissions.SETTLE_VIEW;
    }

    @Override
    public String fileExtension() {
        return "xlsx";
    }

    @Override
    public ExportTaskArtifact generate(ExportTask task, Path target) throws Exception {
        SettleDetailVO detail = settleService.getDetail(task.getSourceUuid());
        try (Workbook workbook = settleExportService.buildWorkbook(detail);
             OutputStream output = Files.newOutputStream(target)) {
            workbook.write(output);
        }
        return new ExportTaskArtifact("结算单_" + detail.getOrder().getSettleNo() + ".xlsx", CONTENT_TYPE);
    }
}
