package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiSawRemainderResolverTest {

    private final ProcessAiSawRemainderResolver resolver = new ProcessAiSawRemainderResolver();

    @Test
    void resolve_whenRemainderIsUnspecified_asksOneGroupedQuestion() {
        ProcessAiExtractionResult resolved = resolver.resolve(
                result(List.of(assignment("R1", "全部切900"),
                        assignment("R2", "全部切900"), assignment("R3", "全部切900"))),
                context(3), "1000的全部切900");

        assertThat(resolved.needsClarification()).isTrue();
        assertThat(resolved.clarificationQuestions()).containsExactly(
                "R1、R2、R3：1000mm母卷切成900mm后，剩余100mm是作为切边余料，还是保留为100mm成品？");
    }

    @Test
    void resolve_whenTrimIsExplicit_keepsWidthsAndDoesNotAsk() {
        ProcessAiExtractionResult resolved = resolver.resolve(
                result(List.of(assignment("R1", "全部切900，剩余作为切边"))),
                context(1), "1000的全部切900，剩余作为切边");

        assertThat(resolved.needsClarification()).isFalse();
        assertThat(resolved.clarificationQuestions()).isEmpty();
        assertThat(resolved.assignments().getFirst().sawIntent().widths())
                .containsExactly(900);
    }

    @Test
    void resolve_whenRemainderIsAProduct_appendsTheFinishWidth() {
        ProcessAiExtractionResult resolved = resolver.resolve(
                result(List.of(assignment("R1", "全部切900，剩余100保留成品"))),
                context(1), "1000的全部切900，剩余100保留成品");

        assertThat(resolved.needsClarification()).isFalse();
        assertThat(resolved.clarificationQuestions()).isEmpty();
        assertThat(resolved.assignments().getFirst().sawIntent().widths())
                .containsExactly(900, 100);
    }

    @Test
    void resolve_whenUserAnswersPreviousQuestion_removesTheStaleQuestion() {
        String previous = "R1：1000mm母卷切成900mm后，剩余100mm是作为切边余料，还是保留为100mm成品？";
        ProcessAiExtractionResult input = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment("R1", "全部切900")),
                List.of(), List.of(), true, List.of(previous));

        ProcessAiExtractionResult resolved = resolver.resolve(
                input, context(1), "所有剩余的都算是余料");

        assertThat(resolved.needsClarification()).isFalse();
        assertThat(resolved.clarificationQuestions()).isEmpty();
        assertThat(resolved.assignments().getFirst().sawIntent().widths()).containsExactly(900);
    }

    private ProcessAiExtractionResult result(List<ProcessAiAssignment> assignments) {
        return new ProcessAiExtractionResult(
                "parse-1", "1.0", assignments, List.of(), List.of(), false, List.of());
    }

    private ProcessAiAssignment assignment(String ref, String evidence) {
        return new ProcessAiAssignment(
                List.of(ref), ref, List.of(), "SAW", null,
                new ProcessAiSawIntent("EXPLICIT_WIDTHS", null, List.of(900), "mm"),
                null, List.of(new ProcessAiEvidence("sawIntent", evidence)));
    }

    private ProcessAiOrderContext context(int count) {
        List<ProcessAiRollContext> rolls = java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(index -> new ProcessAiRollContext(
                        "R" + index, "roll-" + index, index, "白卡纸", 80,
                        1000, 1200, 3, new BigDecimal("500"), 1, 1, 2))
                .toList();
        return new ProcessAiOrderContext("order-1", 3, null, rolls);
    }
}
