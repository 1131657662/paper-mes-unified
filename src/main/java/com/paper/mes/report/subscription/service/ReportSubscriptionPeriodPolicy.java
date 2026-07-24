package com.paper.mes.report.subscription.service;

import com.paper.mes.report.dto.ReportQuery;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public final class ReportSubscriptionPeriodPolicy {

    private ReportSubscriptionPeriodPolicy() {
    }

    public static ReportQuery resolve(ReportQuery source, int policy, LocalDate scheduledDate) {
        ReportQuery query = copy(source);
        switch (policy) {
            case 1 -> assign(query, scheduledDate.minusDays(1), scheduledDate.minusDays(1));
            case 2 -> previousWeek(query, scheduledDate);
            case 3 -> previousMonth(query, scheduledDate);
            case 4 -> requireFixed(query);
            default -> throw new IllegalArgumentException("Unsupported period policy");
        }
        return query;
    }

    private static void previousWeek(ReportQuery query, LocalDate date) {
        LocalDate end = date.with(TemporalAdjusters.previous(DayOfWeek.MONDAY)).minusDays(1);
        assign(query, end.minusDays(6), end);
    }

    private static void previousMonth(ReportQuery query, LocalDate date) {
        LocalDate start = date.withDayOfMonth(1).minusMonths(1);
        assign(query, start, start.with(TemporalAdjusters.lastDayOfMonth()));
    }

    private static void requireFixed(ReportQuery query) {
        if (query.getDateFrom() == null || query.getDateTo() == null) {
            throw new IllegalArgumentException("Fixed period requires both dates");
        }
    }

    private static void assign(ReportQuery query, LocalDate from, LocalDate to) {
        query.setDateFrom(from);
        query.setDateTo(to);
    }

    private static ReportQuery copy(ReportQuery source) {
        ReportQuery target = new ReportQuery();
        target.setCustomerUuid(source.getCustomerUuid());
        target.setPaperName(source.getPaperName());
        target.setMainStepType(source.getMainStepType());
        target.setProcessMode(source.getProcessMode());
        target.setMachineUuid(source.getMachineUuid());
        target.setSettleType(source.getSettleType());
        target.setIsInvoice(source.getIsInvoice());
        target.setOrderStatus(source.getOrderStatus());
        target.setDimension(source.getDimension());
        target.setDateFrom(source.getDateFrom());
        target.setDateTo(source.getDateTo());
        target.setMetricReleaseUuid(source.getMetricReleaseUuid());
        return target;
    }
}
