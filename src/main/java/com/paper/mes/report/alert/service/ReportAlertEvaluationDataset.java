package com.paper.mes.report.alert.service;

import com.paper.mes.report.alert.entity.ReportAlertRule;

import java.util.Map;

public record ReportAlertEvaluationDataset(
        Map<String, ReportAlertMetricSnapshot> snapshots,
        Map<String, String> scopeLabels
) {
    public ReportAlertMetricSnapshot snapshot(ReportAlertRule rule) {
        return snapshots.get(scopeKey(rule));
    }

    public String scopeLabel(ReportAlertRule rule) {
        return scopeLabels.getOrDefault(scopeKey(rule), "未知范围");
    }

    public String canonicalDimensions(ReportAlertRule rule) {
        return scopeKey(rule);
    }

    public static String scopeKey(ReportAlertRule rule) {
        return switch (rule.getScopeType()) {
            case 2 -> "CUSTOMER:" + rule.getCustomerUuid();
            case 3 -> "PAPER:" + rule.getPaperUuid();
            case 4 -> "PROCESS:" + rule.getProcessType();
            default -> "GLOBAL";
        };
    }
}
