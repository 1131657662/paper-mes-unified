package com.paper.mes.report.subscription;

import com.paper.mes.report.subscription.entity.ReportSubscription;
import com.paper.mes.report.subscription.service.ReportSubscriptionSchedulePolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportSubscriptionSchedulePolicyTest {

    @Test
    void nextRun_dailyAfterExecution_movesToTomorrow() {
        ReportSubscription subscription = subscription(1, LocalTime.of(8, 0), null, null);

        LocalDateTime result = ReportSubscriptionSchedulePolicy.nextRun(
                subscription, LocalDateTime.of(2026, 7, 20, 9, 0));

        assertEquals(LocalDateTime.of(2026, 7, 21, 8, 0), result);
    }

    @Test
    void nextRun_weeklyBeforeExecution_keepsCurrentWeek() {
        ReportSubscription subscription = subscription(2, LocalTime.of(8, 0), 1, null);

        LocalDateTime result = ReportSubscriptionSchedulePolicy.nextRun(
                subscription, LocalDateTime.of(2026, 7, 20, 7, 0));

        assertEquals(LocalDateTime.of(2026, 7, 20, 8, 0), result);
    }

    @Test
    void nextRun_monthlyAfterExecution_movesToNextMonth() {
        ReportSubscription subscription = subscription(3, LocalTime.of(8, 0), null, 20);

        LocalDateTime result = ReportSubscriptionSchedulePolicy.nextRun(
                subscription, LocalDateTime.of(2026, 7, 20, 8, 0));

        assertEquals(LocalDateTime.of(2026, 8, 20, 8, 0), result);
    }

    private ReportSubscription subscription(int type, LocalTime time, Integer weekDay, Integer monthDay) {
        ReportSubscription subscription = new ReportSubscription();
        subscription.setScheduleType(type);
        subscription.setExecutionTime(time);
        subscription.setWeekDay(weekDay);
        subscription.setMonthDay(monthDay);
        subscription.setTimezone("Asia/Shanghai");
        return subscription;
    }
}
