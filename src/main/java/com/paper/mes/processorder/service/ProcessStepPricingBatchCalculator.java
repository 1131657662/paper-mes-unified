package com.paper.mes.processorder.service;

import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.ProcessStepPricingBatchPreviewVO;
import com.paper.mes.processorder.entity.ProcessStep;

import java.math.BigDecimal;

public final class ProcessStepPricingBatchCalculator {

    private ProcessStepPricingBatchCalculator() {
    }

    public static ProcessStepPricingBatchPreviewVO.Row preview(ProcessStep step, BigDecimal billingUnitPrice) {
        BigDecimal standardPrice = moneyPrice(step.getUnitPrice());
        BigDecimal currentPrice = effectivePrice(step.getUnitPrice(), step.getBillingUnitPrice());
        BigDecimal finalPrice = effectivePrice(step.getUnitPrice(), billingUnitPrice);
        BigDecimal quantity = billingQuantity(step);
        BigDecimal standardAmount = amount(step, standardPrice, standardQuantity(step));
        BigDecimal currentAmount = amountForMode(step, currentPrice, quantity, standardAmount);
        BigDecimal finalAmount = amountForMode(step, finalPrice, quantity, standardAmount);

        ProcessStepPricingBatchPreviewVO.Row row = new ProcessStepPricingBatchPreviewVO.Row();
        row.setStepUuid(step.getUuid());
        row.setOriginalUuid(step.getOriginalUuid());
        row.setStepType(step.getStepType());
        row.setStepName(step.getStepName());
        row.setQuantity(quantity);
        row.setStandardUnitPrice(standardPrice);
        row.setCurrentUnitPrice(currentPrice);
        row.setFinalUnitPrice(finalPrice);
        row.setStandardAmount(standardAmount);
        row.setCurrentAmount(currentAmount);
        row.setFinalAmount(finalAmount);
        row.setAdjustmentAmount(finalAmount.subtract(standardAmount).setScale(2));
        return row;
    }

    private static BigDecimal amountForMode(ProcessStep step, BigDecimal price, BigDecimal quantity,
                                             BigDecimal standardAmount) {
        int mode = step.getBillingMode() == null ? ProcessStepPricingPolicy.STANDARD : step.getBillingMode();
        if (mode == ProcessStepPricingPolicy.FIXED_AMOUNT) return money(step.getBillingAmount());
        if (mode == ProcessStepPricingPolicy.FREE) return BigDecimal.ZERO.setScale(0);
        return amount(step, price, quantity == null ? standardQuantity(step) : quantity);
    }

    private static BigDecimal amount(ProcessStep step, BigDecimal price, BigDecimal quantity) {
        Integer knives = step.getStepType() != null && step.getStepType() == FeeCalculator.STEP_TYPE_SAW
                ? quantity.intValue() : null;
        BigDecimal weight = step.getStepType() != null && step.getStepType() == FeeCalculator.STEP_TYPE_REWIND
                ? quantity : null;
        return FeeCalculator.stepAmount(step.getStepType(), knives, weight, price);
    }

    private static BigDecimal standardQuantity(ProcessStep step) {
        if (step.getStandardQuantity() != null) return step.getStandardQuantity();
        if (step.getStepType() != null && step.getStepType() == FeeCalculator.STEP_TYPE_SAW) {
            return BigDecimal.valueOf(step.getKnifeCount() == null ? 0 : step.getKnifeCount());
        }
        return step.getProcessWeight() == null ? BigDecimal.ZERO : step.getProcessWeight();
    }

    private static BigDecimal billingQuantity(ProcessStep step) {
        return step.getBillingQuantity() == null ? standardQuantity(step) : step.getBillingQuantity();
    }

    public static BigDecimal effectivePrice(BigDecimal standardPrice, BigDecimal billingPrice) {
        return billingPrice == null ? moneyPrice(standardPrice) : moneyPrice(billingPrice);
    }

    private static BigDecimal moneyPrice(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(0, java.math.RoundingMode.HALF_UP);
    }
}
