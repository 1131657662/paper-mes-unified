package com.paper.mes.report.alert.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReportAlertRuleVO(
        String uuid,
        String signalCode,
        String ruleName,
        Integer scopeType,
        String customerUuid,
        String paperUuid,
        Integer processType,
        String comparisonOperator,
        BigDecimal thresholdValue,
        Integer severity,
        Integer isEnabled,
        Integer version,
        LocalDateTime updateTime
) {
}
