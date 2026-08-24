package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiQuantityIntent;
import com.paper.mes.ai.process.intent.ProcessAiSourceAllocation;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessAiQuantityExpansionServiceTest {

    private final ProcessAiQuantityExpansionService service =
            new ProcessAiQuantityExpansionService();

    @Test
    void perSourceExpands800By3ForEverySelectedMotherRoll() {
        ProcessAiQuantityIntent intent = new ProcessAiQuantityIntent(
                "REPEAT_WIDTH", BigDecimal.valueOf(800), 3, "PER_SOURCE", List.of());

        List<ProcessAiQuantityExpansion> result = service.expand(
                intent, List.of("R1", "R2", "R3"));

        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(value -> assertThat(value.widthsMm())
                .containsExactly(800, 800, 800));
    }

    @Test
    void totalRequiresClosedSourceAllocation() {
        ProcessAiQuantityIntent intent = new ProcessAiQuantityIntent(
                "REPEAT_WIDTH", BigDecimal.valueOf(800), 3, "TOTAL",
                List.of(new ProcessAiSourceAllocation("R1", 2)));

        assertThatThrownBy(() -> service.expand(intent, List.of("R1", "R2")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("全单数量");
    }
}
