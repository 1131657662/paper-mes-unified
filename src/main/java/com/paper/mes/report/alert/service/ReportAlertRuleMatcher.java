package com.paper.mes.report.alert.service;

import com.paper.mes.report.alert.entity.ReportAlertRule;
import com.paper.mes.report.dto.ReportQuery;

import java.util.Comparator;
import java.util.List;

public final class ReportAlertRuleMatcher {
    private ReportAlertRuleMatcher() {
    }

    public static ReportAlertRule best(List<ReportAlertRule> rules, String signalCode,
                                       ReportQuery query, String paperUuid) {
        return rules.stream().filter(rule -> signalCode.equals(rule.getSignalCode()))
                .filter(rule -> matches(rule, query, paperUuid))
                .max(Comparator.comparingInt(rule -> rank(rule.getScopeType())))
                .orElse(null);
    }

    private static boolean matches(ReportAlertRule rule, ReportQuery query, String paperUuid) {
        return switch (rule.getScopeType()) {
            case 1 -> true;
            case 2 -> rule.getCustomerUuid().equals(query.getCustomerUuid());
            case 3 -> rule.getPaperUuid().equals(paperUuid);
            case 4 -> rule.getProcessType().equals(query.getMainStepType());
            default -> false;
        };
    }

    private static int rank(Integer scopeType) {
        return switch (scopeType) {
            case 2 -> 400;
            case 3 -> 300;
            case 4 -> 200;
            case 1 -> 100;
            default -> 0;
        };
    }
}
