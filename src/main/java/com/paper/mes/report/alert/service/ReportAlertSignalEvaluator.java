package com.paper.mes.report.alert.service;

import java.math.BigDecimal;
import java.util.Optional;

public final class ReportAlertSignalEvaluator {
    private ReportAlertSignalEvaluator() {
    }

    public static Optional<BigDecimal> value(String signalCode, ReportAlertMetricSnapshot snapshot) {
        return switch (signalCode) {
            case ReportAlertThresholdService.LOSS_RATIO -> Optional.of(snapshot.lossRatio());
            case ReportAlertThresholdService.UNRECEIVED_RATIO -> Optional.of(snapshot.unreceivedRatio());
            default -> Optional.empty();
        };
    }

    public static boolean matches(String operator, BigDecimal value, BigDecimal threshold) {
        int comparison = value.compareTo(threshold);
        return switch (operator) {
            case "GT" -> comparison > 0;
            case "GTE" -> comparison >= 0;
            case "LT" -> comparison < 0;
            case "LTE" -> comparison <= 0;
            default -> false;
        };
    }
}
