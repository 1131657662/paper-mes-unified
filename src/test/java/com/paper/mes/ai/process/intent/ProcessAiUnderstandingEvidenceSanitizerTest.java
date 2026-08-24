package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiUnderstandingEvidenceSanitizerTest {

    private final ProcessAiUnderstandingEvidenceSanitizer sanitizer =
            new ProcessAiUnderstandingEvidenceSanitizer(new ProcessTextRedactor());

    @Test
    void preservesOnlyEvidenceThatMatchesTheBoundContext() {
        ProcessAiUnderstandingResult result = result(List.of(
                new ProcessAiUnderstandingEvidence("quantityScope", "全单共3卷",
                        "CUSTOMER_TEXT", "customerRequirement", "全单共3卷"),
                new ProcessAiUnderstandingEvidence("widthMm", "2400",
                        "DB_FACT", "R1.widthMm", null),
                new ProcessAiUnderstandingEvidence("quantityScope", "模型猜测",
                        "CUSTOMER_TEXT", "otherText", null)));

        ProcessAiUnderstandingResult sanitized = sanitizer.sanitize(
                result, "复卷800*3，全单共3卷", order(), List.of("rule-1"));

        assertThat(sanitized.evidence()).extracting(ProcessAiUnderstandingEvidence::sourceType)
                .containsExactly("CUSTOMER_TEXT", "DB_FACT", "MODEL_INFERENCE");
        assertThat(sanitized.evidence().get(2).sourceRef()).isEqualTo("model-inference");
    }

    @Test
    void keepsApprovedMemoryReferenceOnlyWhenSelectedByTheServer() {
        ProcessAiUnderstandingResult result = result(List.of(
                new ProcessAiUnderstandingEvidence("mode", "规则说明", "APPROVED_MEMORY",
                        "rule-1", null)));

        assertThat(sanitizer.sanitize(result, "要求", order(), List.of("rule-1"))
                .evidence().get(0).sourceType()).isEqualTo("APPROVED_MEMORY");
        assertThat(sanitizer.sanitize(result, "要求", order(), List.of())
                .evidence().get(0).sourceType()).isEqualTo("MODEL_INFERENCE");
    }

    @Test
    void marksNumericEvidenceAsInferenceWhenItOnlyMatchesPartOfALargerNumber() {
        ProcessAiUnderstandingResult result = result(List.of(
                new ProcessAiUnderstandingEvidence("gramWeight", "80", "CUSTOMER_TEXT",
                        "customerRequirement", null)));

        ProcessAiUnderstandingResult sanitized = sanitizer.sanitize(
                result, "客户要求克重800g", order(), List.of());

        assertThat(sanitized.evidence().getFirst().sourceType()).isEqualTo("MODEL_INFERENCE");
    }

    private ProcessAiUnderstandingResult result(List<ProcessAiUnderstandingEvidence> evidence) {
        return new ProcessAiUnderstandingResult("parse-1", "2.0", "需要确认", evidence,
                List.of(), List.of(), List.of(), true);
    }

    private ProcessAiOrderContext order() {
        return new ProcessAiOrderContext("order-1", 1, "", List.of(new ProcessAiRollContext(
                "R1", "roll-1", 1, "白卡纸", 250, 2400, 1500, 76,
                new BigDecimal("1000"), 1, 1, 2)), null);
    }
}
