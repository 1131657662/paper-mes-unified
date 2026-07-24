package com.paper.mes.report.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ReportQuerySnapshotVO(
        String querySnapshotUuid,
        String queryId,
        String queryHash,
        String metricReleaseUuid,
        Map<String, String> metricVersionMap,
        LocalDateTime dataAsOf,
        LocalDateTime sourceWatermark,
        LocalDateTime expiresAt,
        String scopeHash,
        String consistencyMode,
        String coverage,
        List<String> warnings,
        Map<String, String> sectionStatuses
) {
}
