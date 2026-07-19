package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderServiceImplDetailStepOrderTest {

    @Test
    void sortStepsForDetail_ordersByRollThenStageAndStep() {
        List<OriginalRoll> rolls = List.of(roll("roll-1", 1), roll("roll-2", 2));
        List<ProcessStep> steps = new ArrayList<>(List.of(
                step("step-4", "roll-2", 1, 1),
                step("step-2", "roll-1", 2, 1),
                step("step-3", "roll-1", 2, 2),
                step("step-1", "roll-1", 1, 1)));

        ProcessOrderServiceImpl.sortStepsForDetail(steps, rolls);

        assertThat(steps).extracting(ProcessStep::getUuid)
                .containsExactly("step-1", "step-2", "step-3", "step-4");
    }

    private OriginalRoll roll(String uuid, int rowSort) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setRowSort(rowSort);
        return roll;
    }

    private ProcessStep step(String uuid, String originalUuid, int stageLevel, int stepSort) {
        ProcessStep step = new ProcessStep();
        step.setUuid(uuid);
        step.setOriginalUuid(originalUuid);
        step.setStageLevel(stageLevel);
        step.setStepSort(stepSort);
        return step;
    }
}
