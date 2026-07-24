package com.paper.mes.report.dto;

public record ReportExportExecutionRequest(
        String reportPath,
        ReportQuery query,
        ReportQuerySnapshotVO submissionSnapshot
) {
}
