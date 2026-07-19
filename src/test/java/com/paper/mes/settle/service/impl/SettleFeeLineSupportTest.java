package com.paper.mes.settle.service.impl;

import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SettleFeeLineSupportTest {

    @Test
    void pricingFormula_whenQuantityOverride_explainsStandardQuantityAndReason() {
        ProcessStep step = new ProcessStep();
        step.setBillingMode(2);
        step.setStandardQuantity(new BigDecimal("3.700"));
        step.setPricingAdjustmentAmount(new BigDecimal("-270"));
        step.setPricingAdjustmentReason("客户仅加工20米");
        step.setUnitPrice(new BigDecimal("100"));

        String formula = SettleFeeLineSupport.pricingFormula(step, "1t × 100元/t", "t");

        assertThat(formula).contains("标准 3.7t");
        assertThat(formula).contains("调整 -270");
        assertThat(formula).contains("客户仅加工20米");
    }
}
