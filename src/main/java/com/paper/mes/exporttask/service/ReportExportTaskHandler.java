package com.paper.mes.exporttask.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.dto.ReportExportTaskPayload;
import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportExportExecutionRequest;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;

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
        var payload = payload(task);
        if (payload == null) {
            reportService.exportWorkbook(objectMapper.readValue(task.getRequestPayload(), ReportQuery.class), target);
        } else {
            reportService.exportWorkbook(new ReportExportExecutionRequest(
                    payload.reportPath(), payload.query(), payload.submissionSnapshot()), target);
        }
        return new ExportTaskArtifact("统计报表_" + LocalDate.now() + ".xlsx", CONTENT_TYPE);
    }

    private ReportExportTaskPayload payload(ExportTask task) throws Exception {
        var tree = objectMapper.readTree(task.getRequestPayload());
        if (!tree.has("schemaVersion")) return null;
        ReportExportTaskPayload payload = objectMapper.treeToValue(tree, ReportExportTaskPayload.class);
        if (payload.schemaVersion() != ReportExportTaskPayload.CURRENT_SCHEMA_VERSION
                || payload.submissionSnapshot() == null || payload.query() == null
                || !Objects.equals(payload.querySnapshotUuid(), payload.submissionSnapshot().querySnapshotUuid())
                || !Objects.equals(payload.querySnapshotUuid(), task.getQuerySnapshotUuid())
                || !Objects.equals(payload.reportPath(), task.getSourcePath())
                || !Objects.equals(payload.submissionSnapshot().metricReleaseUuid(), task.getMetricReleaseUuid())) {
            throw new BusinessException("报表导出任务快照已损坏，请重新发起导出");
        }
        return payload;
    }
}
