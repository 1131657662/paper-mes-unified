package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.settle.entity.SettleOrder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SettleCollectionQueryPolicyTest {

    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SettleOrder.class);
    }

    @Test
    void apply_todayQueue_excludesAlreadyRemindedOrdersAndMatchesDueDate() {
        LambdaQueryWrapper<SettleOrder> wrapper = wrapper(SettleCollectionQueryPolicy.TODAY);

        assertThat(wrapper.getSqlSegment()).contains("settle_status", "unreceived_amount", "due_date");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(LocalDate.of(2026, 8, 2));
    }

    @Test
    void apply_overdueQueue_usesBeforeTodayBoundary() {
        LambdaQueryWrapper<SettleOrder> wrapper = wrapper(SettleCollectionQueryPolicy.OVERDUE);

        assertThat(wrapper.getSqlSegment()).contains("due_date <");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(LocalDate.of(2026, 8, 2));
    }

    @Test
    void apply_upcomingQueue_includesUndatedOrders() {
        LambdaQueryWrapper<SettleOrder> wrapper = wrapper(SettleCollectionQueryPolicy.UPCOMING);

        assertThat(wrapper.getSqlSegment()).contains("due_date", "IS NULL");
    }

    @Test
    void apply_remindedQueue_matchesTodayReminderWithoutDueDateFilter() {
        LambdaQueryWrapper<SettleOrder> wrapper = wrapper(SettleCollectionQueryPolicy.REMINDED);

        assertThat(wrapper.getSqlSegment()).contains("last_reminder_time >=").doesNotContain("due_date =");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(LocalDate.of(2026, 8, 2).atStartOfDay());
    }

    private LambdaQueryWrapper<SettleOrder> wrapper(String queue) {
        LambdaQueryWrapper<SettleOrder> wrapper = new LambdaQueryWrapper<>();
        SettleCollectionQueryPolicy.apply(wrapper, queue, LocalDate.of(2026, 8, 2));
        return wrapper;
    }
}
