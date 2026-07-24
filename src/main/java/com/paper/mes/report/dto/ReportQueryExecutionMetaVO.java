package com.paper.mes.report.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ReportQueryExecutionMetaVO(
        String queryId,
        String queryHash,
        String metricReleaseUuid,
        Map<String, String> metricVersionMap,
        LocalDateTime dataAsOf,
        LocalDateTime sourceWatermark,
        String consistencyMode,
        String coverage,
        List<String> warnings,
        Map<String, String> sectionStatuses
) {
}
