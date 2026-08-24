package com.paper.mes.processorder.service;

import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiPlanFieldMergerTest {

    private final ProcessAiPlanFieldMerger merger = new ProcessAiPlanFieldMerger();

    @Test
    void mergeExistingDiameterPreservesSegmentCountOrderAndUnselectedCore() {
        ProcessPlanDTO current = plan(segment(1, "60", 1100, 6), segment(2, "40", 1200, 6));
        ProcessPlanDTO candidate = plan(segment(1, "50", 1000, 3));

        ProcessPlanDTO result = merger.merge(current, candidate(candidate), List.of(
                "/assignments/R1/rewindIntent/diameterRule/type"));

        assertThat(result.getSegments()).hasSize(2);
        assertThat(result.getSegments()).extracting(RewindSegmentPlanDTO::getSegmentSort)
                .containsExactly(1, 2);
        assertThat(result.getSegments()).extracting(RewindSegmentPlanDTO::getTargetDiameter)
                .containsExactly(1000, 1000);
        assertThat(result.getSegments()).extracting(RewindSegmentPlanDTO::getFinishCoreDiameter)
                .containsExactly(6, 6);
    }

    @Test
    void mergeNewPlanDoesNotApplyUnselectedCandidateFields() {
        ProcessPlanDTO candidate = plan(segment(1, "50", 1000, 3));

        ProcessPlanDTO result = merger.merge(null, candidate(candidate), List.of(
                "/assignments/R1/processType",
                "/assignments/R1/sourceRollRefs"));

        assertThat(result.getProcessMode()).isEqualTo(1);
        assertThat(result.getMainStepType()).isEqualTo(2);
        assertThat(result.getSegments()).singleElement().satisfies(segment -> {
            assertThat(segment.getTargetDiameter()).isNull();
            assertThat(segment.getFinishCoreDiameter()).isNull();
        });
    }

    @Test
    void mergePreservesCurrentMachineWhenMachineWasNotAccepted() {
        ProcessPlanDTO current = plan(segment(1, "100", 1200, 3));
        current.setMachineUuid("old-machine");
        ProcessPlanDTO proposed = plan(segment(1, "100", 1200, 3));
        proposed.setMachineUuid("resolved-machine");

        ProcessPlanDTO result = merger.merge(current, candidate(proposed), List.of(
                "/assignments/R1/rewindIntent/diameterRule/type"));

        assertThat(result.getMachineUuid()).isEqualTo("old-machine");
    }

    @Test
    void mergePersistsTheBackendResolvedMachineWhenAccepted() {
        ProcessPlanDTO current = plan(segment(1, "100", 1200, 3));
        current.setMachineUuid("old-machine");
        ProcessPlanDTO proposed = plan(segment(1, "100", 1200, 3));
        proposed.setMachineUuid("resolved-machine");

        ProcessPlanDTO result = merger.merge(current, candidate(proposed), List.of(
                "/assignments/R1/machineUuid"));

        assertThat(result.getMachineUuid()).isEqualTo("resolved-machine");
    }

    @Test
    void mergeAcceptedProcessModeLeavesTheExistingMainProcessUntouched() {
        ProcessPlanDTO current = plan(segment(1, "100", 1200, 3));
        current.setProcessMode(1);
        current.setMainStepType(2);
        ProcessPlanDTO proposed = plan(segment(1, "100", 1200, 3));
        proposed.setProcessMode(2);
        proposed.setMainStepType(1);

        ProcessPlanDTO result = merger.merge(current, candidate(proposed), List.of(
                "/assignments/R1/processMode"));

        assertThat(result.getProcessMode()).isEqualTo(2);
        assertThat(result.getMainStepType()).isEqualTo(2);
    }

    @Test
    void mergeAcceptedProcessTypeLeavesTheExistingProcessModeUntouched() {
        ProcessPlanDTO current = plan(segment(1, "100", 1200, 3));
        current.setProcessMode(2);
        current.setMainStepType(1);
        ProcessPlanDTO proposed = plan(segment(1, "100", 1200, 3));
        proposed.setProcessMode(1);
        proposed.setMainStepType(2);

        ProcessPlanDTO result = merger.merge(current, candidate(proposed), List.of(
                "/assignments/R1/processType"));

        assertThat(result.getProcessMode()).isEqualTo(2);
        assertThat(result.getMainStepType()).isEqualTo(2);
    }

    @Test
    void changingToWidthOnlyClearsDiameterCoreAndWeightSplitAllocation() {
        ProcessPlanDTO current = plan(segment(1, "100", 1200, 3));
        current.setRewindMode(3);
        current.setAllocationRule("WEIGHT_SPLIT");
        current.setWidthDifferencePolicy("REMAINDER");
        ProcessPlanDTO proposed = plan(segment(1, "100", 900, 3));
        proposed.setRewindMode(1);
        proposed.setAllocationRule(null);
        proposed.setWidthDifferencePolicy("REMAINDER");

        ProcessPlanDTO result = merger.merge(current, candidate(proposed), List.of(
                "/assignments/R1/rewindIntent/modeIntent"));

        assertThat(result.getRewindMode()).isEqualTo(1);
        assertThat(result.getAllocationRule()).isNull();
        assertThat(result.getWidthDifferencePolicy()).isEqualTo("REMAINDER");
        assertThat(result.getSegments()).singleElement().satisfies(segment -> {
            assertThat(segment.getTargetDiameter()).isNull();
            assertThat(segment.getFinishCoreDiameter()).isNull();
        });
    }

    private ProcessAiCompiledPlan candidate(ProcessPlanDTO plan) {
        return new ProcessAiCompiledPlan("R1", "roll-1", List.of(), plan, null);
    }

    private ProcessPlanDTO plan(RewindSegmentPlanDTO... segments) {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setMainStepType(2);
        plan.setRewindMode(2);
        plan.setSegments(List.of(segments));
        return plan;
    }

    private RewindSegmentPlanDTO segment(int sort, String ratio, int diameter, int core) {
        RewindSegmentPlanDTO segment = new RewindSegmentPlanDTO();
        segment.setSegmentSort(sort);
        segment.setSegmentRatio(new BigDecimal(ratio));
        segment.setTargetDiameter(diameter);
        segment.setFinishCoreDiameter(core);
        segment.setRepeatCount(1);
        segment.setSources(List.of());
        segment.setLayoutItems(List.of());
        return segment;
    }

}
