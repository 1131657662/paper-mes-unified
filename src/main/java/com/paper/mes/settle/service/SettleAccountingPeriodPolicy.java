package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.processorder.entity.ProcessOrder;

import java.time.LocalDate;

/** Defines the business date used to assign completed orders to settlement periods. */
public final class SettleAccountingPeriodPolicy {

    private static final String ACCOUNTING_DATE_SQL = "DATE(COALESCE(back_record_time, order_date))";

    private SettleAccountingPeriodPolicy() {
    }

    public static void applyPeriod(LambdaQueryWrapper<ProcessOrder> wrapper,
                                   LocalDate start, LocalDate end) {
        if (start != null) wrapper.apply(ACCOUNTING_DATE_SQL + " >= {0}", start);
        if (end != null) wrapper.apply(ACCOUNTING_DATE_SQL + " <= {0}", end);
    }

    public static void orderByAccountingDate(LambdaQueryWrapper<ProcessOrder> wrapper) {
        wrapper.last("ORDER BY COALESCE(back_record_time, order_date) ASC, order_no ASC");
    }

    public static LocalDate accountingDate(ProcessOrder order) {
        if (order.getBackRecordTime() != null) return order.getBackRecordTime().toLocalDate();
        return order.getOrderDate();
    }
}
