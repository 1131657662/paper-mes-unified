package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiGroupedPiecePlanGuardTest {

    private final ProcessAiGroupedPiecePlanGuard guard = new ProcessAiGroupedPiecePlanGuard();

    @Test
    void resolve_whenAllGroupedPiecesUseSameWidth_normalizesToOneRecipe() {
        ProcessAiExtractionResult resolved = guard.resolve(result(widths(12, 900)), context(12));

        assertThat(resolved.needsClarification()).isFalse();
        assertThat(resolved.assignments().getFirst().sawIntent().widths()).containsExactly(900);
    }

    @Test
    void resolve_whenGroupedPiecesNeedDifferentWidths_requiresSourceSplit() {
        List<Integer> widths = new ArrayList<>();
        widths.addAll(widths(9, 900));
        widths.addAll(widths(3, 850));

        ProcessAiExtractionResult resolved = guard.resolve(result(widths), context(12));

        assertThat(resolved.needsClarification()).isTrue();
        assertThat(resolved.clarificationQuestions()).containsExactly(
                "R1 是合并录入的 12 件母卷，但要求 9件切900mm、3件切850mm，"
                        + "同一母卷记录只能保存一套工艺。请返回第2步拆成对应的母卷行后重新解析。");
    }

    @Test
    void resolve_whenWidthsFitOneSourcePiece_keepsTheSawRecipe() {
        ProcessAiExtractionResult resolved = guard.resolve(result(List.of(600, 400)), context(12));

        assertThat(resolved.needsClarification()).isFalse();
        assertThat(resolved.assignments().getFirst().sawIntent().widths())
                .containsExactly(600, 400);
    }

    @Test
    void resolvedGroupedPlanRemovesThePreviousSplitQuestion() {
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW", null,
                new ProcessAiSawIntent("EXPLICIT_WIDTHS", null, List.of(900), "mm"), null,
                List.of(new ProcessAiEvidence("sawIntent", "1000的9件切900")));
        ProcessAiExtractionResult input = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment), List.of(), List.of(), true,
                List.of("R1 是合并录入的 12 件母卷，但要求 9件切900mm、3件切850mm，"
                        + "同一母卷记录只能保存一套工艺。请返回第2步拆成对应的母卷行后重新解析。"));

        ProcessAiExtractionResult resolved = guard.resolve(input, context(9));

        assertThat(resolved.needsClarification()).isFalse();
        assertThat(resolved.clarificationQuestions()).isEmpty();
    }

    private ProcessAiExtractionResult result(List<Integer> widths) {
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW", null,
                new ProcessAiSawIntent("EXPLICIT_WIDTHS", null, widths, "mm"), null,
                List.of(new ProcessAiEvidence("sawIntent", "1000的9件切900,3件切850")));
        return new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment), List.of(), List.of(), false, List.of());
    }

    private ProcessAiOrderContext context(int pieces) {
        ProcessAiRollContext roll = new ProcessAiRollContext(
                "R1", "roll-1", 1, "测试", 80, 1000, 1200, 3,
                new BigDecimal("1000"), pieces, 1, 2);
        return new ProcessAiOrderContext("order-1", 1, null, List.of(roll));
    }

    private List<Integer> widths(int count, int width) {
        return java.util.stream.IntStream.range(0, count).map(ignored -> width).boxed().toList();
    }
}
