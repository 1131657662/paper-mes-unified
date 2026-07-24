package com.paper.mes.report.dto;

public record ReportMetricItemVO(
        String metricUuid,
        String metricCode,
        String metricName,
        String description,
        String valueType,
        String unitCode,
        int displayScale,
        String metricVersionUuid,
        int versionNo,
        String definitionChecksum
) {
}
