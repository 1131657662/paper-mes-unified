package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiSourceAssignmentResolverTest {

    private final ProcessAiSourceAssignmentResolver resolver =
            new ProcessAiSourceAssignmentResolver();

    @Test
    void resolvesRepeatedModelReferenceByWidthAndPieceCount() {
        ProcessAiExtractionResult result = result(List.of(
                saw("R1", 900, "1000的9件切900"),
                rewind("R2", "1250的直径一分为二"),
                saw("R1", 850, "3件切850")));

        ProcessAiExtractionResult resolved = resolver.resolve(result, context());

        assertThat(resolved.needsClarification()).isFalse();
        assertThat(resolved.assignments()).extracting(ProcessAiAssignment::ownerRollRef)
                .containsExactly("R1", "R2", "R3");
        assertThat(resolved.assignments().get(2).sourceRollRefs()).containsExactly("R3");
    }

    @Test
    void requestsClarificationWhenPieceCountDoesNotIdentifyOneRoll() {
        ProcessAiExtractionResult result = result(List.of(
                saw("R1", 900, "1000的全部切900"),
                saw("R1", 850, "1000的全部切850")));

        ProcessAiExtractionResult resolved = resolver.resolve(result, context());

        assertThat(resolved.needsClarification()).isTrue();
        assertThat(resolved.clarificationQuestions()).anyMatch(
                question -> question.contains("无法根据门幅和未识别件数唯一匹配母卷行"));
    }

    @Test
    void splitsOneFlattenedSawAssignmentAcrossMatchingGroupedRollRows() {
        List<Integer> widths = new java.util.ArrayList<>();
        for (int index = 0; index < 9; index++) widths.add(900);
        for (int index = 0; index < 3; index++) widths.add(850);
        ProcessAiAssignment combined = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW", null,
                new ProcessAiSawIntent("EXPLICIT_WIDTHS", null, widths, "mm"), null,
                List.of(new ProcessAiEvidence("sawIntent", "1000的9件切900，3件切850")));

        ProcessAiExtractionResult resolved = resolver.resolve(result(List.of(combined)), context());

        assertThat(resolved.assignments()).hasSize(2);
        assertThat(resolved.assignments()).extracting(ProcessAiAssignment::ownerRollRef)
                .containsExactly("R1", "R3");
        assertThat(resolved.assignments().get(0).sawIntent().widths()).containsExactly(900);
        assertThat(resolved.assignments().get(1).sawIntent().widths()).containsExactly(850);
    }

    @Test
    void resolvedBindingsRemoveThePreviousBindingQuestion() {
        ProcessAiExtractionResult input = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(
                saw("R1", 900, "1000的9件切900"),
                saw("R1", 850, "3件切850")),
                List.of(), List.of(), true, List.of(
                "R1 被多组工艺重复引用，且无法根据门幅和未识别件数唯一匹配母卷行。请说明该组对应哪一条母卷。"));

        ProcessAiExtractionResult resolved = resolver.resolve(input, context());

        assertThat(resolved.needsClarification()).isFalse();
        assertThat(resolved.clarificationQuestions()).isEmpty();
        assertThat(resolved.assignments()).extracting(ProcessAiAssignment::ownerRollRef)
                .containsExactly("R1", "R3");
    }

    private ProcessAiExtractionResult result(List<ProcessAiAssignment> assignments) {
        return new ProcessAiExtractionResult(
                "parse-1", "1.0", assignments, List.of(), List.of(), false, List.of());
    }

    private ProcessAiAssignment saw(String ref, int width, String evidence) {
        ProcessAiSawIntent intent = new ProcessAiSawIntent(
                "EXPLICIT_WIDTHS", null, List.of(width), "mm");
        return new ProcessAiAssignment(
                List.of(ref), ref, List.of(), "SAW", null, intent, null,
                List.of(new ProcessAiEvidence("sawIntent", evidence)));
    }

    private ProcessAiAssignment rewind(String ref, String evidence) {
        ProcessAiDiameterRule diameter = new ProcessAiDiameterRule(
                "WEIGHT_SPLIT", 2, List.of(new BigDecimal("50"), new BigDecimal("50")),
                new ProcessAiMeasurement(new BigDecimal("1200"), "mm", "DEFAULT"));
        ProcessAiRewindIntent intent = new ProcessAiRewindIntent(
                "CHANGE_DIAMETER", diameter, null, null);
        return new ProcessAiAssignment(
                List.of(ref), ref, List.of(), "REWIND", intent, null, null,
                List.of(new ProcessAiEvidence("diameterRule", evidence)));
    }

    private ProcessAiOrderContext context() {
        return new ProcessAiOrderContext("order-1", 7, "客户原话", List.of(
                roll("R1", "roll-1", 1, 1000, 9),
                roll("R2", "roll-2", 2, 1250, 5),
                roll("R3", "roll-3", 3, 1000, 3)));
    }

    private ProcessAiRollContext roll(String ref, String uuid, int sort,
                                      int width, int pieces) {
        return new ProcessAiRollContext(ref, uuid, sort, "白卡纸", 250,
                width, 1500, 3, new BigDecimal("1000"), pieces, 1, 2);
    }
}
