package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.settle.entity.SettleOrder;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class SettleCollectionQueryPolicy {
    public static final String TODAY = "today";
    public static final String OVERDUE = "overdue";
    public static final String UPCOMING = "upcoming";
    public static final String REMINDED = "reminded";

    private SettleCollectionQueryPolicy() {
    }

    public static void apply(LambdaQueryWrapper<SettleOrder> wrapper, String queue, LocalDate today) {
        LocalDateTime start = today.atStartOfDay();
        wrapper.in(SettleOrder::getSettleStatus, 1, 2)
                .gt(SettleOrder::getUnreceivedAmount, 0);
        if (REMINDED.equals(queue)) {
            wrapper.ge(SettleOrder::getLastReminderTime, start);
            return;
        }
        wrapper.and(item -> item.isNull(SettleOrder::getLastReminderTime)
                .or().lt(SettleOrder::getLastReminderTime, start));
        if (TODAY.equals(queue)) wrapper.eq(SettleOrder::getDueDate, today);
        if (OVERDUE.equals(queue)) wrapper.lt(SettleOrder::getDueDate, today);
        if (UPCOMING.equals(queue)) {
            wrapper.and(item -> item.gt(SettleOrder::getDueDate, today).or().isNull(SettleOrder::getDueDate));
        }
    }
}
