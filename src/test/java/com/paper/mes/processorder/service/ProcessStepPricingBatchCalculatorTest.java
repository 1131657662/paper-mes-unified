package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessStepPricingBatchPreviewVO;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessStepPricingBatchCalculatorTest {

    @Test
    void preview_rewindOverride_usesTonUnitAndPreservesStandardAmount() {
        ProcessStep step = step(2, "3.700", "100");

        ProcessStepPricingBatchPreviewVO.Row row = ProcessStepPricingBatchCalculator.preview(
                step, new BigDecimal("80"));

        assertThat(row.getStandardAmount()).isEqualByComparingTo("370");
        assertThat(row.getFinalAmount()).isEqualByComparingTo("296");
        assertThat(row.getAdjustmentAmount()).isEqualByComparingTo("-74");
    }

    @Test
    void preview_sawOverride_usesKnifeCount() {
        ProcessStep step = step(1, "3", "120");

        ProcessStepPricingBatchPreviewVO.Row row = ProcessStepPricingBatchCalculator.preview(
                step, new BigDecimal("100"));

        assertThat(row.getFinalAmount()).isEqualByComparingTo("300");
    }

    @Test
    void preview_restoreStandard_removesExistingPriceOverride() {
        ProcessStep step = step(2, "3.700", "100");
        step.setBillingUnitPrice(new BigDecimal("80"));

        ProcessStepPricingBatchPreviewVO.Row row = ProcessStepPricingBatchCalculator.preview(step, null);

        assertThat(row.getCurrentAmount()).isEqualByComparingTo("296");
        assertThat(row.getFinalAmount()).isEqualByComparingTo("370");
    }

    private ProcessStep step(int type, String quantity, String price) {
        ProcessStep step = new ProcessStep();
        step.setUuid("step-" + type);
        step.setStepType(type);
        step.setBillingMode(ProcessStepPricingPolicy.STANDARD);
        step.setUnitPrice(new BigDecimal(price));
        step.setStandardQuantity(new BigDecimal(quantity));
        step.setKnifeCount(type == 1 ? Integer.valueOf(quantity) : null);
        step.setProcessWeight(type == 2 ? new BigDecimal(quantity) : null);
        return step;
    }
}
