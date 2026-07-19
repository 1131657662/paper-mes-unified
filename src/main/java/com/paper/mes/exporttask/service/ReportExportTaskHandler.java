package com.paper.mes.exporttask.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ReportExportTaskHandler implements ExportTaskHandler {
    public static final String TASK_TYPE = "REPORT_FULL";
    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ObjectMapper objectMapper;
    private final ReportService reportService;

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public String requiredPermission() {
        return Permissions.REPORT_VIEW;
    }

    @Override
    public String fileExtension() {
        return "xlsx";
    }

    @Override
    public ExportTaskArtifact generate(ExportTask task, Path target) throws Exception {
        ReportQuery query = objectMapper.readValue(task.getRequestPayload(), ReportQuery.class);
        reportService.exportWorkbook(query, target);
        return new ExportTaskArtifact("统计报表_" + LocalDate.now() + ".xlsx", CONTENT_TYPE);
    }
}
