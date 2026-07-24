package com.paper.mes.report.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ReportExportAuditMetadata(
        String reportPath,
        String querySnapshotUuid,
        LocalDateTime submissionDataAsOf,
        LocalDateTime executionDataAsOf,
        String metricReleaseUuid,
        Map<String, String> metricVersionMap
) {
}
