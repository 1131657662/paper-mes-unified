package com.paper.mes.report.alert.service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public record ReportAlertEvaluationWindow(
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate asOf
) {
    public static ReportAlertEvaluationWindow currentMonth(LocalDate asOf) {
        return new ReportAlertEvaluationWindow(asOf.withDayOfMonth(1),
                asOf.with(TemporalAdjusters.lastDayOfMonth()), asOf);
    }
}
