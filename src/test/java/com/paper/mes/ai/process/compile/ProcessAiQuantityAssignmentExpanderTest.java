package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiQuantityIntent;
import com.paper.mes.ai.process.intent.ProcessAiRewindIntent;
import com.paper.mes.ai.process.intent.ProcessAiSourceAllocation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiQuantityAssignmentExpanderTest {

    private final ProcessAiQuantityAssignmentExpander expander =
            new ProcessAiQuantityAssignmentExpander(new ProcessAiQuantityExpansionService());

    @Test
    void expand_perSourceQuantity_createsThreeIndependentEightHundredByThreePlans() {
        List<ProcessAiAssignment> expanded = expander.expand(extraction(new ProcessAiQuantityIntent(
                "REPEAT_WIDTH", new BigDecimal("800"), 3, "PER_SOURCE", List.of())));

        assertThat(expanded).extracting(ProcessAiAssignment::ownerRollRef)
                .containsExactly("R1", "R2", "R3");
        assertThat(expanded).allSatisfy(assignment -> {
            assertThat(assignment.sourceRollRefs()).containsExactly(assignment.ownerRollRef());
            assertThat(assignment.rewindIntent().quantityIntent().count()).isEqualTo(3);
            assertThat(assignment.rewindIntent().modeIntent()).isEqualTo("CHANGE_WIDTH");
        });
    }

    @Test
    void expand_totalQuantity_usesOnlyTheClosedServerValidatedAllocation() {
        List<ProcessAiAssignment> expanded = expander.expand(extraction(new ProcessAiQuantityIntent(
                "REPEAT_WIDTH", new BigDecimal("800"), 5, "TOTAL", List.of(
                new ProcessAiSourceAllocation("R1", 2),
                new ProcessAiSourceAllocation("R2", 3)))));

        assertThat(expanded).extracting(ProcessAiAssignment::ownerRollRef)
                .containsExactly("R1", "R2");
        assertThat(expanded).extracting(item -> item.rewindIntent().quantityIntent().count())
                .containsExactly(2, 3);
    }

    private ProcessAiExtractionResult extraction(ProcessAiQuantityIntent quantity) {
        ProcessAiRewindIntent rewind = new ProcessAiRewindIntent(
                "MULTI_SOURCE", null, null, null, quantity);
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1", "R2", "R3"), "R1", List.of(), "REWIND", rewind,
                null, null, List.of());
        return new ProcessAiExtractionResult("parse-1", "1.0", List.of(assignment),
                List.of(), List.of(), false, List.of());
    }
}
