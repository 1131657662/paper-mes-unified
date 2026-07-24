package com.paper.mes.report.subscription;

import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.subscription.service.ReportSubscriptionPeriodPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportSubscriptionPeriodPolicyTest {

    @Test
    void resolve_previousWeekCrossingMonth_returnsMondayThroughSunday() {
        ReportQuery result = ReportSubscriptionPeriodPolicy.resolve(
                new ReportQuery(), 2, LocalDate.of(2026, 7, 1));

        assertEquals(LocalDate.of(2026, 6, 22), result.getDateFrom());
        assertEquals(LocalDate.of(2026, 6, 28), result.getDateTo());
    }

    @Test
    void resolve_previousMonthAtYearBoundary_returnsDecember() {
        ReportQuery result = ReportSubscriptionPeriodPolicy.resolve(
                new ReportQuery(), 3, LocalDate.of(2026, 1, 3));

        assertEquals(LocalDate.of(2025, 12, 1), result.getDateFrom());
        assertEquals(LocalDate.of(2025, 12, 31), result.getDateTo());
    }

    @Test
    void resolve_previousDay_doesNotMutateSavedFilter() {
        ReportQuery saved = new ReportQuery();

        ReportQuery result = ReportSubscriptionPeriodPolicy.resolve(saved, 1, LocalDate.of(2026, 7, 20));

        assertEquals(LocalDate.of(2026, 7, 19), result.getDateFrom());
        assertEquals(null, saved.getDateFrom());
    }
}
