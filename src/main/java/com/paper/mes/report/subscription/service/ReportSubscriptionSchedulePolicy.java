package com.paper.mes.report.subscription.service;

import com.paper.mes.report.subscription.entity.ReportSubscription;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

public final class ReportSubscriptionSchedulePolicy {
    public static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Shanghai");

    private ReportSubscriptionSchedulePolicy() {
    }

    public static LocalDateTime nextRun(ReportSubscription subscription, LocalDateTime after) {
        ZoneId zone = ZoneId.of(subscription.getTimezone());
        ZonedDateTime zonedAfter = after.atZone(STORAGE_ZONE).withZoneSameInstant(zone);
        ZonedDateTime candidate = candidate(subscription, zonedAfter.toLocalDate());
        if (!candidate.isAfter(zonedAfter)) candidate = advance(subscription, candidate);
        return candidate.withZoneSameInstant(STORAGE_ZONE).toLocalDateTime();
    }

    private static ZonedDateTime candidate(ReportSubscription subscription, LocalDate date) {
        LocalDate scheduledDate = switch (subscription.getScheduleType()) {
            case 1 -> date;
            case 2 -> date.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(subscription.getWeekDay())));
            case 3 -> date.withDayOfMonth(subscription.getMonthDay());
            default -> throw new IllegalArgumentException("Unsupported schedule type");
        };
        LocalTime time = subscription.getExecutionTime();
        return scheduledDate.atTime(time).atZone(ZoneId.of(subscription.getTimezone()));
    }

    private static ZonedDateTime advance(ReportSubscription subscription, ZonedDateTime candidate) {
        return switch (subscription.getScheduleType()) {
            case 1 -> candidate.plusDays(1);
            case 2 -> candidate.plusWeeks(1);
            case 3 -> candidate.plusMonths(1);
            default -> throw new IllegalArgumentException("Unsupported schedule type");
        };
    }
}
