package com.paper.mes.processorder.service;

import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessMixProcessResolverTest {

    @Test
    void isMix_whenSameRollHasTwoSameTypeSteps_returnsTrue() {
        List<ProcessStep> steps = List.of(
                step("roll-1", FeeCalculator.STEP_TYPE_SAW),
                step("roll-1", FeeCalculator.STEP_TYPE_SAW)
        );

        assertTrue(ProcessMixProcessResolver.isMix(steps));
    }

    @Test
    void isMix_whenOrderHasSawAndRewind_returnsTrue() {
        List<ProcessStep> steps = List.of(
                step("roll-1", FeeCalculator.STEP_TYPE_SAW),
                step("roll-2", FeeCalculator.STEP_TYPE_REWIND)
        );

        assertTrue(ProcessMixProcessResolver.isMix(steps));
    }

    @Test
    void isMix_whenEachRollHasSingleSameTypeStep_returnsFalse() {
        List<ProcessStep> steps = List.of(
                step("roll-1", FeeCalculator.STEP_TYPE_SAW),
                step("roll-2", FeeCalculator.STEP_TYPE_SAW)
        );

        assertFalse(ProcessMixProcessResolver.isMix(steps));
    }

    private ProcessStep step(String originalUuid, Integer stepType) {
        ProcessStep step = new ProcessStep();
        step.setOriginalUuid(originalUuid);
        step.setStepType(stepType);
        return step;
    }
}
