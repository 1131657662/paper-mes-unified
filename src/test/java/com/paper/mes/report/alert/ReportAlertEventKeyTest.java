package com.paper.mes.report.alert;

import com.paper.mes.report.alert.service.ReportAlertEventKey;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReportAlertEventKeyTest {
    @Test
    void generate_sameRuleReleasePeriodAndDimensions_isStable() {
        String dimensions = ReportAlertEventKey.dimensionHash("customer=1|paper=all");

        String first = ReportAlertEventKey.generate("rule", "release",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), dimensions);
        String second = ReportAlertEventKey.generate("rule", "release",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), dimensions);

        assertThat(first).isEqualTo(second).hasSize(64);
    }
}
