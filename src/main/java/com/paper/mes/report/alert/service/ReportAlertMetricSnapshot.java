package com.paper.mes.report.alert.service;

import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ReportAlertMetricSnapshot(
        long orderCount,
        BigDecimal lossRatio,
        BigDecimal settledAmount,
        BigDecimal unreceivedAmount
) {
    public static ReportAlertMetricSnapshot from(ReportOverviewVO value) {
        return new ReportAlertMetricSnapshot(count(value.getOrderCount()), decimal(value.getLossRatio()),
                decimal(value.getSettledAmount()), decimal(value.getUnreceivedAmount()));
    }

    public static ReportAlertMetricSnapshot from(ReportDimensionVO value) {
        return new ReportAlertMetricSnapshot(count(value.getOrderCount()), decimal(value.getLossRatio()),
                decimal(value.getSettledAmount()), decimal(value.getUnreceivedAmount()));
    }

    public boolean hasData() {
        return orderCount > 0;
    }

    public BigDecimal unreceivedRatio() {
        if (settledAmount.signum() <= 0) return BigDecimal.ZERO;
        return unreceivedAmount.multiply(BigDecimal.valueOf(100))
                .divide(settledAmount, 6, RoundingMode.HALF_UP);
    }

    private static long count(Long value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal decimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
