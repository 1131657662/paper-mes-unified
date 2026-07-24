package com.paper.mes.report.alert;

import com.paper.mes.report.alert.service.ReportAlertMetricSnapshot;
import com.paper.mes.report.alert.service.ReportAlertSignalEvaluator;
import com.paper.mes.report.alert.service.ReportAlertThresholdService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ReportAlertSignalEvaluatorTest {
    @Test
    void value_unreceivedRatio_usesSettledAmountAsDenominator() {
        var snapshot = snapshot("4.52", "200", "70");

        BigDecimal value = ReportAlertSignalEvaluator.value(
                ReportAlertThresholdService.UNRECEIVED_RATIO, snapshot).orElseThrow();

        assertThat(value).isEqualByComparingTo("35.000000");
    }

    @ParameterizedTest
    @CsvSource({"GT,5.01,5,true", "GTE,5,5,true", "LT,4.99,5,true", "LTE,5,5,true",
            "GT,5,5,false", "LT,5,5,false"})
    void matches_supportedOperators_returnsExpected(
            String operator, String value, String threshold, boolean expected) {
        assertThat(ReportAlertSignalEvaluator.matches(operator,
                new BigDecimal(value), new BigDecimal(threshold))).isEqualTo(expected);
    }

    private ReportAlertMetricSnapshot snapshot(String lossRatio, String settled, String unreceived) {
        return new ReportAlertMetricSnapshot(1, new BigDecimal(lossRatio),
                new BigDecimal(settled), new BigDecimal(unreceived));
    }
}
