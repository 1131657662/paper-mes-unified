package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SettleCandidateAmountLoader {

    private static final int STEP_TYPE_SAW = 1;
    private static final int STEP_TYPE_REWIND = 2;
    private static final int MONEY_SCALE = 2;

    private final ProcessStepMapper processStepMapper;

    public Map<String, CandidateAmount> load(List<ProcessOrder> orders) {
        if (orders.isEmpty()) return Map.of();
        Map<String, CandidateAmount> amounts = initialize(orders);
        List<String> orderUuids = orders.stream().map(ProcessOrder::getUuid).toList();
        List<ProcessStep> steps = processStepMapper.selectList(new LambdaQueryWrapper<ProcessStep>()
                .in(ProcessStep::getOrderUuid, orderUuids));
        for (ProcessStep step : steps) {
            amounts.computeIfPresent(step.getOrderUuid(), (key, amount) -> amount.add(step));
        }
        return amounts;
    }

    private Map<String, CandidateAmount> initialize(List<ProcessOrder> orders) {
        Map<String, CandidateAmount> amounts = new LinkedHashMap<>();
        for (ProcessOrder order : orders) {
            amounts.put(order.getUuid(), new CandidateAmount(BigDecimal.ZERO, BigDecimal.ZERO,
                    money(order.getTotalExtraAmount()), money(order.getTotalAmount())));
        }
        return amounts;
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public record CandidateAmount(BigDecimal saw, BigDecimal rewind, BigDecimal extra, BigDecimal total) {
        CandidateAmount add(ProcessStep step) {
            BigDecimal amount = money(step.getStepAmount());
            if (step.getStepType() != null && step.getStepType() == STEP_TYPE_SAW) {
                return new CandidateAmount(saw.add(amount), rewind, extra, total);
            }
            if (step.getStepType() != null && step.getStepType() == STEP_TYPE_REWIND) {
                return new CandidateAmount(saw, rewind.add(amount), extra, total);
            }
            return this;
        }

        public BigDecimal effectiveTotal() {
            if (total.signum() > 0) return total;
            return saw.add(rewind).add(extra).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
    }
}
