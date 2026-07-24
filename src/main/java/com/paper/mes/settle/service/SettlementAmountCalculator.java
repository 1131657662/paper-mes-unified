package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.settle.entity.SettleDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SettlementAmountCalculator {

    private static final int STEP_TYPE_SAW = 1;
    private static final int STEP_TYPE_REWIND = 2;
    private static final int STEP_TYPE_STRIP_SORT = 3;
    private static final int STEP_TYPE_REPACKAGE = 4;
    private static final int MONEY_SCALE = 2;

    private final ProcessStepMapper processStepMapper;

    public Calculation calculate(List<ProcessOrder> orders, Integer isInvoice, Customer customer) {
        Map<String, StepAmounts> steps = loadSteps(orders);
        List<SettleDetail> details = new ArrayList<>(orders.size());
        Totals totals = Totals.zero();
        for (ProcessOrder order : orders) {
            StepAmounts amount = steps.getOrDefault(order.getUuid(), StepAmounts.zero());
            SettleDetail detail = detail(order, amount, isInvoice, customer);
            details.add(detail);
            totals = totals.add(detail, noTax(order, detail), tax(order, detail, isInvoice, customer));
        }
        return totals.calculation(details);
    }

    private Map<String, StepAmounts> loadSteps(List<ProcessOrder> orders) {
        if (orders.isEmpty()) return Map.of();
        List<String> orderUuids = orders.stream().map(ProcessOrder::getUuid).toList();
        List<ProcessStep> rows = processStepMapper.selectList(new LambdaQueryWrapper<ProcessStep>()
                .in(ProcessStep::getOrderUuid, orderUuids));
        Map<String, StepAmounts> grouped = new LinkedHashMap<>();
        for (ProcessStep row : rows) {
            grouped.merge(row.getOrderUuid(), StepAmounts.of(row), StepAmounts::add);
        }
        return grouped;
    }

    private SettleDetail detail(ProcessOrder order, StepAmounts steps, Integer isInvoice, Customer customer) {
        SettleDetail detail = new SettleDetail();
        detail.setOrderUuid(order.getUuid());
        detail.setOrderNo(order.getOrderNo());
        detail.setSawAmount(steps.saw());
        detail.setRewindAmount(steps.rewind());
        detail.setServiceAmount(steps.service());
        detail.setStandardProcessAmount(steps.standardProcess());
        detail.setPricingAdjustmentAmount(steps.pricingAdjustment());
        detail.setPricingAdjustmentReason(steps.reason());
        detail.setExtraAmount(money(order.getTotalExtraAmount()));
        BigDecimal noTax = noTax(order, detail);
        detail.setOrderAmount(noTax.add(tax(order, detail, isInvoice, customer)).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        return detail;
    }

    private BigDecimal noTax(ProcessOrder order, SettleDetail detail) {
        if (order.getTotalAmountNoTax() != null) return money(order.getTotalAmountNoTax());
        return money(detail.getSawAmount()).add(money(detail.getRewindAmount()))
                .add(money(detail.getServiceAmount())).add(money(detail.getExtraAmount()));
    }

    private BigDecimal tax(ProcessOrder order, SettleDetail detail, Integer isInvoice, Customer customer) {
        if (isInvoice == null || isInvoice != 1) return BigDecimal.ZERO.setScale(MONEY_SCALE);
        return FeeCalculator.tax(noTax(order, detail), taxRate(order, customer), true);
    }

    private BigDecimal taxRate(ProcessOrder order, Customer customer) {
        if (order.getTaxRate() != null) return order.getTaxRate();
        return customer == null ? BigDecimal.ZERO : money(customer.getTaxRate());
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public record Calculation(BigDecimal saw, BigDecimal rewind, BigDecimal service, BigDecimal extra, BigDecimal noTax,
                              BigDecimal tax, BigDecimal total, int pendingPriceCount,
                              List<SettleDetail> details) {
    }

    private record StepAmounts(BigDecimal saw, BigDecimal rewind, BigDecimal service, BigDecimal standardProcess,
                               BigDecimal pricingAdjustment, String reason) {
        static StepAmounts zero() {
            return new StepAmounts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        }

        static StepAmounts of(ProcessStep step) {
            BigDecimal value = money(step.getStepAmount());
            BigDecimal standard = money(step.getStandardStepAmount() == null
                    ? step.getStepAmount() : step.getStandardStepAmount());
            BigDecimal adjustment = money(step.getPricingAdjustmentAmount());
            String reason = step.getPricingAdjustmentReason();
            if (step.getStepType() != null && step.getStepType() == STEP_TYPE_SAW) {
                return new StepAmounts(value, BigDecimal.ZERO, BigDecimal.ZERO, standard, adjustment, reason);
            }
            if (step.getStepType() != null && step.getStepType() == STEP_TYPE_REWIND) {
                return new StepAmounts(BigDecimal.ZERO, value, BigDecimal.ZERO, standard, adjustment, reason);
            }
            if (step.getStepType() != null && (step.getStepType() == STEP_TYPE_STRIP_SORT
                    || step.getStepType() == STEP_TYPE_REPACKAGE)) {
                return new StepAmounts(BigDecimal.ZERO, BigDecimal.ZERO, value, standard, adjustment, reason);
            }
            return zero();
        }

        StepAmounts add(StepAmounts other) {
            return new StepAmounts(saw.add(other.saw), rewind.add(other.rewind), service.add(other.service),
                    standardProcess.add(other.standardProcess), pricingAdjustment.add(other.pricingAdjustment),
                    joinReason(reason, other.reason));
        }

        private static String joinReason(String left, String right) {
            if (left == null || left.isBlank()) return right;
            if (right == null || right.isBlank() || left.equals(right)) return left;
            return left + "；" + right;
        }
    }

    private record Totals(BigDecimal saw, BigDecimal rewind, BigDecimal service, BigDecimal extra, BigDecimal noTax,
                          BigDecimal tax, BigDecimal total) {
        static Totals zero() {
            return new Totals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Totals add(SettleDetail detail, BigDecimal lineNoTax, BigDecimal lineTax) {
            return new Totals(saw.add(detail.getSawAmount()), rewind.add(detail.getRewindAmount()),
                    service.add(detail.getServiceAmount()),
                    extra.add(detail.getExtraAmount()), noTax.add(lineNoTax), tax.add(lineTax),
                    total.add(detail.getOrderAmount()));
        }

        Calculation calculation(List<SettleDetail> details) {
            int pendingPriceCount = (int) details.stream()
                    .filter(detail -> detail.getOrderAmount().signum() <= 0)
                    .count();
            return new Calculation(saw, rewind, service, extra, noTax, tax, total, pendingPriceCount, details);
        }
    }
}
