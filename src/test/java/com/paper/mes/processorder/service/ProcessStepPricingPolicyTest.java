package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessStepPricingPolicyTest {

    @Test
    void quantityOverride_whenActualIsThreePointSevenTons_chargesOneTon() {
        ProcessStep step = step(2, 2);
        step.setBillingQuantity(new BigDecimal("1.000"));

        ProcessStepPricingPolicy.Result result = ProcessStepPricingPolicy.calculate(
                step, new BigDecimal("3.700"), new BigDecimal("370"), new BigDecimal("100"));

        assertThat(result.billingQuantity()).isEqualByComparingTo("1.000");
        assertThat(result.finalAmount()).isEqualByComparingTo("100");
        assertThat(result.adjustmentAmount()).isEqualByComparingTo("-270");
    }

    @Test
    void standardMode_whenNoAdjustment_keepsTheOriginalAmount() {
        ProcessStep step = step(2, 1);

        ProcessStepPricingPolicy.Result result = ProcessStepPricingPolicy.calculate(
                step, new BigDecimal("3.700"), new BigDecimal("370"), new BigDecimal("100"));

        assertThat(result.billingQuantity()).isEqualByComparingTo("3.700");
        assertThat(result.finalAmount()).isEqualByComparingTo("370");
        assertThat(result.adjustmentAmount()).isZero();
    }

    @Test
    void standardMode_whenQuantityIsNotConfigured_returnsZeroAmount() {
        ProcessStep step = step(2, 1);

        ProcessStepPricingPolicy.Result result = ProcessStepPricingPolicy.calculate(
                step, null, BigDecimal.ZERO, new BigDecimal("100"));

        assertThat(result.billingQuantity()).isNull();
        assertThat(result.finalAmount()).isZero();
        assertThat(result.adjustmentAmount()).isZero();
    }

    @Test
    void quantityOverride_whenAmountHasFractionalYuan_keepsEngineRoundingToWholeYuan() {
        ProcessStep step = step(2, 2);
        step.setBillingQuantity(new BigDecimal("3.700"));

        ProcessStepPricingPolicy.Result result = ProcessStepPricingPolicy.calculate(
                step, new BigDecimal("4.000"), new BigDecimal("722"), new BigDecimal("180.55"));

        assertThat(result.finalAmount()).isEqualByComparingTo("668");
        assertThat(result.adjustmentAmount()).isEqualByComparingTo("-54");
    }

    @Test
    void quantityOverride_whenSawQuantityIsFractional_rejectsInvalidBillingUnit() {
        ProcessStep step = step(1, 2);
        step.setBillingQuantity(new BigDecimal("1.500"));

        assertThatThrownBy(() -> ProcessStepPricingPolicy.calculate(
                step, new BigDecimal("3"), new BigDecimal("300"), new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("整数刀数");
    }

    private ProcessStep step(int type, int mode) {
        ProcessStep step = new ProcessStep();
        step.setStepType(type);
        step.setKnifeCount(type == 1 ? 3 : null);
        step.setBillingMode(mode);
        return step;
    }
}
