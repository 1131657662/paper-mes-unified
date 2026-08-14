package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BackRecordWeightRequirementPolicyTest {

    @Test
    void requiredRollUuids_withMergedStandardRewind_requiresEverySourceRoll() {
        List<OriginalRoll> rolls = List.of(roll("roll-1"), roll("roll-2"), roll("roll-3"));
        ProcessStep step = step(2, ProcessStepPricingPolicy.STANDARD);
        List<FinishOriginalRel> relations = List.of(
                relation("finish-1", "roll-1"),
                relation("finish-1", "roll-2"),
                relation("finish-1", "roll-3"));

        Set<String> required = BackRecordWeightRequirementPolicy.requiredRollUuids(
                rolls, List.of(step), relations);

        assertThat(required).containsExactly("roll-1", "roll-2", "roll-3");
    }

    @Test
    void requiredRollUuids_withSawStep_doesNotRequireWeight() {
        Set<String> required = BackRecordWeightRequirementPolicy.requiredRollUuids(
                List.of(roll("roll-1")), List.of(step(1, ProcessStepPricingPolicy.STANDARD)), List.of());

        assertThat(required).isEmpty();
    }

    @Test
    void requiredRollUuids_withNonStandardRewindModes_doesNotRequireWeight() {
        for (int mode : List.of(
                ProcessStepPricingPolicy.QUANTITY_OVERRIDE,
                ProcessStepPricingPolicy.FIXED_AMOUNT,
                ProcessStepPricingPolicy.FREE)) {
            Set<String> required = BackRecordWeightRequirementPolicy.requiredRollUuids(
                    List.of(roll("roll-1")), List.of(step(2, mode)), List.of());
            assertThat(required).as("billing mode %s", mode).isEmpty();
        }
    }

    private OriginalRoll roll(String uuid) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        return roll;
    }

    private ProcessStep step(int type, int mode) {
        ProcessStep step = new ProcessStep();
        step.setUuid("step-1");
        step.setOriginalUuid("roll-1");
        step.setStepType(type);
        step.setBillingMode(mode);
        return step;
    }

    private FinishOriginalRel relation(String finishUuid, String originalUuid) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setFinishUuid(finishUuid);
        relation.setOriginalUuid(originalUuid);
        return relation;
    }
}
