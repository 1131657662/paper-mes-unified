package com.paper.mes.settle.service.impl;

import com.paper.mes.settle.dto.SettleListSummaryVO;
import com.paper.mes.settle.entity.SettleOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

final class SettleListSummaryAccumulator {

    private long pending;
    private long partial;
    private long paid;
    private long voided;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal received = BigDecimal.ZERO;
    private BigDecimal unreceived = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;

    static SettleListSummaryVO summarize(List<SettleOrder> settlements) {
        SettleListSummaryAccumulator accumulator = new SettleListSummaryAccumulator();
        settlements.forEach(accumulator::add);
        return accumulator.toView(settlements.size());
    }

    private void add(SettleOrder settlement) {
        int status = settlement.getSettleStatus() == null ? 0 : settlement.getSettleStatus();
        if (status == 1) pending++;
        if (status == 2) partial++;
        if (status == 3) paid++;
        if (status == 4) voided++;
        if (status >= 1 && status <= 3) addActiveAmounts(settlement);
    }

    private void addActiveAmounts(SettleOrder settlement) {
        total = total.add(money(settlement.getTotalAmount()));
        received = received.add(money(settlement.getReceivedAmount()));
        unreceived = unreceived.add(money(settlement.getUnreceivedAmount()));
        discount = discount.add(money(settlement.getDiscountAmount()));
    }

    private SettleListSummaryVO toView(long documentCount) {
        return new SettleListSummaryVO(documentCount, pending, partial, paid, voided,
                total, received, unreceived, discount, LocalDateTime.now());
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
