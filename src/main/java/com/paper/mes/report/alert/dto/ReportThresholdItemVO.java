package com.paper.mes.report.alert.dto;

import java.math.BigDecimal;

public record ReportThresholdItemVO(
        String ruleUuid,
        String signalCode,
        String comparisonOperator,
        BigDecimal thresholdValue,
        Integer severity,
        Integer scopeType,
        String scopeLabel
) {
}
