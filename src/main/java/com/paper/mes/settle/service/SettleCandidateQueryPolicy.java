package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.settle.dto.SettleCandidateQuery;
import org.springframework.util.StringUtils;

public final class SettleCandidateQueryPolicy {
    private static final int ORDER_STATUS_FINISHED = 4;

    private SettleCandidateQueryPolicy() {
    }

    public static LambdaQueryWrapper<ProcessOrder> create(SettleCandidateQuery query) {
        LambdaQueryWrapper<ProcessOrder> wrapper = new LambdaQueryWrapper<ProcessOrder>()
                .eq(ProcessOrder::getOrderStatus, ORDER_STATUS_FINISHED);
        if (query.getOrderUuids() != null && !query.getOrderUuids().isEmpty()) {
            wrapper.in(ProcessOrder::getUuid, query.getOrderUuids());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(ProcessOrder::getOrderNo, keyword)
                    .or().like(ProcessOrder::getCustomerName, keyword));
        }
        if (StringUtils.hasText(query.getCustomerUuid())) {
            wrapper.eq(ProcessOrder::getCustomerUuid, query.getCustomerUuid());
        }
        SettleAccountingPeriodPolicy.applyPeriod(wrapper, query.getPeriodStart(), query.getPeriodEnd());
        SettleAccountingPeriodPolicy.orderByAccountingDate(wrapper);
        return wrapper;
    }
}
