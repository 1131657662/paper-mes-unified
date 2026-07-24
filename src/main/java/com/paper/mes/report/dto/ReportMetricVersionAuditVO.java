package com.paper.mes.report.dto;

import java.time.LocalDateTime;

public record ReportMetricVersionAuditVO(
        String metricUuid,
        String metricCode,
        String metricName,
        String description,
        String valueType,
        String unitCode,
        int displayScale,
        int displayOrder,
        String metricVersionUuid,
        int versionNo,
        String implementationKey,
        String definitionJson,
        String definitionChecksum,
        int versionStatus,
        LocalDateTime lockedAt,
        String lockedBy
) {
}
