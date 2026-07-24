package com.paper.mes.exporttask.dto;

import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportQuerySnapshotVO;

public record ReportExportTaskPayload(
        int schemaVersion,
        String querySnapshotUuid,
        String reportPath,
        ReportQuery query,
        ReportQuerySnapshotVO submissionSnapshot
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
}
