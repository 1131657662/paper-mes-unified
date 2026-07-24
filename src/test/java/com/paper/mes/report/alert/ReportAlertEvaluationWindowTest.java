package com.paper.mes.report.alert;

import com.paper.mes.report.alert.service.ReportAlertEvaluationWindow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReportAlertEvaluationWindowTest {
    @Test
    void currentMonth_midMonth_usesStableNaturalMonthIdentity() {
        ReportAlertEvaluationWindow window = ReportAlertEvaluationWindow.currentMonth(
                LocalDate.of(2026, 7, 20));

        assertThat(window.periodStart()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(window.periodEnd()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(window.asOf()).isEqualTo(LocalDate.of(2026, 7, 20));
    }
}
