package com.paper.mes.processorder.service;

import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.entity.ProcessStep;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure rules for separating standard production pricing from final billable pricing. */
public final class ProcessStepPricingPolicy {

    public static final int STANDARD = 1;
    public static final int QUANTITY_OVERRIDE = 2;
    public static final int FIXED_AMOUNT = 3;
    public static final int FREE = 4;

    private ProcessStepPricingPolicy() {
    }

    public static Result calculate(ProcessStep step, BigDecimal standardQuantity,
                                   BigDecimal standardAmount, BigDecimal unitPrice) {
        int mode = step.getBillingMode() == null ? STANDARD : step.getBillingMode();
        BigDecimal finalQuantity = standardQuantity;
        BigDecimal finalAmount = standardAmount;
        if (mode == QUANTITY_OVERRIDE) {
            finalQuantity = requireQuantity(step.getBillingQuantity());
            finalAmount = quantityAmount(step, finalQuantity, unitPrice);
        } else if (mode == FIXED_AMOUNT) {
            finalQuantity = null;
            finalAmount = money(step.getBillingAmount());
        } else if (mode == FREE) {
            finalQuantity = null;
            finalAmount = BigDecimal.ZERO.setScale(0);
        } else if (mode != STANDARD) {
            throw new IllegalStateException("Unsupported process step billing mode: " + mode);
        } else {
            finalAmount = quantityAmount(step, standardQuantity, unitPrice);
        }
        BigDecimal adjustment = finalAmount.subtract(standardAmount).setScale(2, RoundingMode.HALF_UP);
        return new Result(mode, standardQuantity, finalQuantity, standardAmount, finalAmount, adjustment);
    }

    private static BigDecimal quantityAmount(ProcessStep step, BigDecimal quantity, BigDecimal unitPrice) {
        if (step.getStepType() == FeeCalculator.STEP_TYPE_SAW) {
            try {
                return FeeCalculator.stepAmount(step.getStepType(), quantity.intValueExact(), null, unitPrice);
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException("锯纸最终计费数量必须为整数刀数");
            }
        }
        return FeeCalculator.stepAmount(step.getStepType(), null, quantity, unitPrice);
    }

    private static BigDecimal requireQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("最终计费数量必须大于0");
        }
        return quantity.setScale(3, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(0, RoundingMode.HALF_UP);
    }

    public record Result(int mode, BigDecimal standardQuantity, BigDecimal billingQuantity,
                         BigDecimal standardAmount, BigDecimal finalAmount,
                         BigDecimal adjustmentAmount) {
    }
}
