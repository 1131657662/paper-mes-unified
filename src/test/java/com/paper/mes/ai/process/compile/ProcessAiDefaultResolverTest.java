package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiRewindIntent;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessAiDefaultResolverTest {

    private final ProcessAiDefaultResolver resolver = new ProcessAiDefaultResolver();

    @Test
    void ordinaryRewindWithoutCoreUsesThreeInchDefault() {
        List<ProcessAiDefaultValue> defaults = resolver.resolve(extraction("CHANGE_WIDTH"),
                context(3));

        assertThat(defaults).extracting(ProcessAiDefaultValue::defaultId)
                .containsExactly("REWIND_FINISH_CORE_3_INCH");
    }

    @Test
    void keepSpecWithoutMotherCoreIsBlocked() {
        assertThatThrownBy(() -> resolver.resolve(extraction("KEEP_SPEC"), context(null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("母卷纸芯");
    }

    private ProcessAiExtractionResult extraction(String mode) {
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "REWIND",
                new ProcessAiRewindIntent(mode, null, null, null), null, null, List.of());
        return new ProcessAiExtractionResult("p1", "1.0", List.of(assignment),
                List.of(), List.of(), false, List.of());
    }

    private ProcessAiOrderContext context(Integer core) {
        ProcessAiRollContext roll = new ProcessAiRollContext(
                "R1", "original-1", 1, "paper", 80, 2400, 1200, core,
                null, 1, 1, 2);
        return new ProcessAiOrderContext("order-1", 3, "", List.of(roll));
    }
}
