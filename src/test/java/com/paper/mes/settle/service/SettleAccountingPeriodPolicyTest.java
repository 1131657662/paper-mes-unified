package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SettleAccountingPeriodPolicyTest {

    @Test
    void accountingDate_usesBackRecordDateWhenPresent() {
        ProcessOrder order = order(LocalDate.of(2026, 7, 1));
        order.setBackRecordTime(LocalDateTime.of(2026, 7, 16, 23, 30));

        LocalDate result = SettleAccountingPeriodPolicy.accountingDate(order);

        assertThat(result).isEqualTo(LocalDate.of(2026, 7, 16));
    }

    @Test
    void accountingDate_fallsBackToOrderDateForLegacyRows() {
        ProcessOrder order = order(LocalDate.of(2026, 6, 30));

        LocalDate result = SettleAccountingPeriodPolicy.accountingDate(order);

        assertThat(result).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void applyPeriod_usesParameterizedAccountingDateExpression() {
        LambdaQueryWrapper<ProcessOrder> wrapper = new LambdaQueryWrapper<>();

        SettleAccountingPeriodPolicy.applyPeriod(
                wrapper, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertThat(wrapper.getSqlSegment())
                .contains("DATE(COALESCE(back_record_time, order_date)) >=")
                .contains("DATE(COALESCE(back_record_time, order_date)) <=");
        assertThat(wrapper.getParamNameValuePairs()).hasSize(2);
    }

    private ProcessOrder order(LocalDate orderDate) {
        ProcessOrder order = new ProcessOrder();
        order.setOrderDate(orderDate);
        return order;
    }
}
