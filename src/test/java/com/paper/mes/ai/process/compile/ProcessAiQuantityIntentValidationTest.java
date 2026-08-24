package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiQuantityIntent;
import com.paper.mes.ai.process.intent.ProcessAiSourceAllocation;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessAiQuantityIntentValidationTest {

    @Test
    void totalExpansionRejectsUnknownSource() {
        ProcessAiQuantityExpansionService service = new ProcessAiQuantityExpansionService();
        ProcessAiQuantityIntent intent = new ProcessAiQuantityIntent(
                "REPEAT_WIDTH", BigDecimal.valueOf(800), 1, "TOTAL",
                List.of(new ProcessAiSourceAllocation("R9", 1)));

        assertThatThrownBy(() -> service.expand(intent, List.of("R1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无效");
    }
}
