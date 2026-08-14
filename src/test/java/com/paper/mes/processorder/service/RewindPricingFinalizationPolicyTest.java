package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RewindPricingFinalizationPolicyTest {

    @Test
    void standardEstimatedRewind_blocksFinalization() {
        ProcessStep step = rewind(ProcessStepPricingPolicy.STANDARD, "ESTIMATED", 0);

        assertThatThrownBy(() -> RewindPricingFinalizationPolicy.requireFinalized(List.of(step)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("实测计费");
    }

    @Test
    void measuredButDirtyRewind_blocksFinalization() {
        ProcessStep step = rewind(ProcessStepPricingPolicy.STANDARD, "MEASURED", 1);

        assertThatThrownBy(() -> RewindPricingFinalizationPolicy.requireFinalized(List.of(step)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重新核定");
    }

    @Test
    void measuredCleanRewind_allowsFinalization() {
        ProcessStep step = rewind(ProcessStepPricingPolicy.STANDARD, "MEASURED", 0);

        assertThatCode(() -> RewindPricingFinalizationPolicy.requireFinalized(List.of(step)))
                .doesNotThrowAnyException();
    }

    @Test
    void explicitNonStandardPricing_doesNotRequireMeasuredTonnage() {
        ProcessStep fixed = rewind(ProcessStepPricingPolicy.FIXED_AMOUNT, "ESTIMATED", 0);
        ProcessStep free = rewind(ProcessStepPricingPolicy.FREE, "PENDING", 0);

        assertThatCode(() -> RewindPricingFinalizationPolicy.requireFinalized(List.of(fixed, free)))
                .doesNotThrowAnyException();
    }

    @Test
    void legacyCompletedRewind_withoutNewStatus_remainsSettleableDuringMigration() {
        ProcessStep legacy = rewind(ProcessStepPricingPolicy.STANDARD, null, 0);

        assertThatCode(() -> RewindPricingFinalizationPolicy.requireFinalizedForSettlement(List.of(legacy)))
                .doesNotThrowAnyException();
    }

    @Test
    void migratedEstimatedRewind_blocksSettlement() {
        ProcessStep migrated = rewind(ProcessStepPricingPolicy.STANDARD, "ESTIMATED", 0);

        assertThatThrownBy(() -> RewindPricingFinalizationPolicy.requireFinalizedForSettlement(List.of(migrated)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("实测计费");
    }

    private ProcessStep rewind(int mode, String status, int dirty) {
        ProcessStep step = new ProcessStep();
        step.setStepType(2);
        step.setBillingMode(mode);
        step.setBillingWeightStatus(status);
        step.setPricingDirty(dirty);
        return step;
    }
}
