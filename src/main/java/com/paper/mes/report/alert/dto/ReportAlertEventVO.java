package com.paper.mes.report.alert.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportAlertEventVO(
        String uuid,
        String ruleName,
        String signalCode,
        String scopeLabel,
        String metricReleaseUuid,
        String comparisonOperator,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal metricValue,
        BigDecimal thresholdValue,
        Integer severity,
        Integer eventStatus,
        Integer occurrenceCount,
        LocalDateTime firstDetectedAt,
        LocalDateTime lastDetectedAt,
        LocalDateTime resolvedAt,
        LocalDateTime acknowledgedAt,
        String acknowledgedBy,
        LocalDateTime ignoredAt,
        String ignoredBy,
        String ignoreReason
) {
}
