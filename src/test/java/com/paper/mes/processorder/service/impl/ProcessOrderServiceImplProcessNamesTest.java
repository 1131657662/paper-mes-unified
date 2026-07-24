package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderServiceImplProcessNamesTest {

    @Test
    void processNames_withMultipleTypes_returnsDistinctNamesInRouteOrder() {
        List<ProcessStep> steps = List.of(
                step(1, null),
                step(3, "剥损整理"),
                step(3, "返工整理"));

        List<String> names = ProcessOrderServiceImpl.processNames(steps, List.of(roll(1)));

        assertThat(names).containsExactly("锯纸", "剥损整理");
    }

    @Test
    void processNames_withoutStepsAndDirectRoll_returnsDirectShip() {
        List<String> names = ProcessOrderServiceImpl.processNames(List.of(), List.of(roll(3)));

        assertThat(names).containsExactly("直发");
    }

    @Test
    void processNames_withoutStepsAndStandardRoll_returnsPendingConfiguration() {
        List<String> names = ProcessOrderServiceImpl.processNames(List.of(), List.of(roll(1)));

        assertThat(names).containsExactly("待配置");
    }

    private ProcessStep step(int type, String name) {
        ProcessStep step = new ProcessStep();
        step.setStepType(type);
        step.setStepName(name);
        return step;
    }

    private OriginalRoll roll(int processMode) {
        OriginalRoll roll = new OriginalRoll();
        roll.setProcessMode(processMode);
        return roll;
    }
}
