package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessStepWeightStatePolicyTest {

    @Test
    void clearWhenNotRewind_removesStaleRewindBillingState() {
        ProcessStep step = step(1);

        ProcessStepWeightStatePolicy.clearWhenNotRewind(step);

        assertThat(step.getProcessWeight()).isNull();
        assertThat(step.getBillingWeightStatus()).isNull();
        assertThat(step.getBillingWeightBasis()).isNull();
        assertThat(step.getPricingDirty()).isZero();
    }

    @Test
    void clearWhenNotRewind_preservesActiveRewindState() {
        ProcessStep step = step(2);

        ProcessStepWeightStatePolicy.clearWhenNotRewind(step);

        assertThat(step.getProcessWeight()).isEqualByComparingTo("2.000");
        assertThat(step.getBillingWeightStatus()).isEqualTo("MEASURED");
        assertThat(step.getBillingWeightBasis()).isEqualTo("INPUT_TOTAL");
        assertThat(step.getPricingDirty()).isEqualTo(1);
    }

    private ProcessStep step(int type) {
        ProcessStep step = new ProcessStep();
        step.setStepType(type);
        step.setProcessWeight(new BigDecimal("2.000"));
        step.setBillingWeightStatus("MEASURED");
        step.setBillingWeightBasis("INPUT_TOTAL");
        step.setPricingDirty(1);
        return step;
    }
}
