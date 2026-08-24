package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiSourceAssignmentResolverAncillaryTest {

    @Test
    void resolveAllAncillaryRequestCreatesOneAssignmentPerSourceRoll() {
        ProcessAiPackagingRequirement packaging = new ProcessAiPackagingRequirement(
                "OTHER", "[金额]", "PIECE", true);
        ProcessAiAssignment template = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "ANCILLARY_ONLY", null, null,
                new ProcessAiAncillaryRequirements(null, packaging), List.of(
                        new ProcessAiEvidence("packaging", "全部剥破损包装，每件20元")));
        ProcessAiExtractionResult input = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(template), List.of(), List.of(), false, List.of());

        ProcessAiExtractionResult result = new ProcessAiSourceAssignmentResolver().resolve(
                input, context(), "不加工，只附加工艺加工，全部剥破损包装，每件[金额]");

        assertThat(result.assignments()).extracting(ProcessAiAssignment::ownerRollRef)
                .containsExactly("R1", "R2");
        assertThat(result.assignments()).allMatch(value ->
                "ANCILLARY_ONLY".equals(value.processType())
                        && value.sourceRollRefs().size() == 1);
    }

    private ProcessAiOrderContext context() {
        return new ProcessAiOrderContext("order-1", 1, null, List.of(
                roll("R1", "roll-1"), roll("R2", "roll-2")));
    }

    private ProcessAiRollContext roll(String ref, String uuid) {
        return new ProcessAiRollContext(ref, uuid, 1, "paper", 250, 800,
                1200, 76, new BigDecimal("500"), 1, 1, 1);
    }
}
