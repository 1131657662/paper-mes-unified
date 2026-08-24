package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiEvidence;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiRewindIntent;
import com.paper.mes.ai.process.intent.ProcessAiQuantityIntent;
import com.paper.mes.ai.process.intent.ProcessAiSourceAllocation;
import com.paper.mes.ai.process.parse.dto.ProcessAiCorrection;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessAiCorrectionServiceTest {

    private final ProcessAiCorrectionService service = new ProcessAiCorrectionService();

    @Test
    void correctionChangesOnlyAllowlistedCoreField() {
        ProcessAiExtractionResult result = extraction();

        ProcessAiExtractionResult revised = service.apply(result,
                List.of(new ProcessAiCorrection("R1", "finishCoreDiameter", 3, "inch")));

        assertThat(revised.assignments().getFirst().rewindIntent().core().value())
                .isEqualByComparingTo("3");
        assertThat(revised.assignments().getFirst().rewindIntent().core().source())
                .isEqualTo("EXPLICIT");
    }

    @Test
    void correctionRejectsArbitraryFieldNames() {
        assertThatThrownBy(() -> service.apply(extraction(),
                List.of(new ProcessAiCorrection("R1", "/rewindIntent/core", 3, "inch"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许");
    }

    @Test
    void correctionRejectsNonMillimetreWidthUnits() {
        assertThatThrownBy(() -> service.apply(extraction(),
                List.of(new ProcessAiCorrection("R1", "widthMm", 800, "inch"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("单位必须为mm");
    }

    @Test
    void correctionRejectsWidthOutsideTheContractRange() {
        assertThatThrownBy(() -> service.apply(extraction(),
                List.of(new ProcessAiCorrection("R1", "widthMm", 10_001, "mm"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超过10000毫米");
    }

    @Test
    void correctionRejectsCoreOutsideTheContractRange() {
        assertThatThrownBy(() -> service.apply(extraction(),
                List.of(new ProcessAiCorrection("R1", "finishCoreDiameter", 10_001, "inch"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超过10000");
    }

    @Test
    void correctionPreservesUnresolvedQuestionsForUnrelatedFields() {
        ProcessAiExtractionResult result = new ProcessAiExtractionResult("p1", "1.0",
                extraction().assignments(), List.of(), List.of(), true,
                List.of("数量范围：每条母卷还是全单合计？"));

        ProcessAiExtractionResult revised = service.apply(result,
                List.of(new ProcessAiCorrection("R1", "finishCoreDiameter", 3, "inch")));

        assertThat(revised.needsClarification()).isTrue();
        assertThat(revised.clarificationQuestions()).containsExactly(
                "数量范围：每条母卷还是全单合计？");
    }

    @Test
    void correctionClearsMatchingQuantityQuestionOnlyWhenScopeIsProvided() {
        ProcessAiQuantityIntent quantity = new ProcessAiQuantityIntent(
                "REPEAT_WIDTH", new java.math.BigDecimal("800"), 3, "PER_SOURCE", List.of());
        ProcessAiAssignment assignment = new ProcessAiAssignment(List.of("R1"), "R1", List.of(),
                "REWIND", new ProcessAiRewindIntent("CHANGE_WIDTH", null, null, null, quantity),
                null, null, List.of(new ProcessAiEvidence("quantity", "800×3")));
        ProcessAiExtractionResult result = new ProcessAiExtractionResult("p1", "1.0",
                List.of(assignment), List.of(), List.of(), true,
                List.of("数量范围：每条母卷还是全单合计？"));

        ProcessAiExtractionResult revised = service.apply(result,
                List.of(new ProcessAiCorrection("R1", "quantityScope", null, "TOTAL", null)));

        assertThat(revised.needsClarification()).isFalse();
        assertThat(revised.clarificationQuestions()).isEmpty();
        assertThat(revised.assignments().getFirst().rewindIntent().quantityIntent().scope())
                .isEqualTo("TOTAL");
    }

    @Test
    void correctionChangingQuantityScopeClearsTheIncompatibleAllocationTable() {
        ProcessAiQuantityIntent quantity = new ProcessAiQuantityIntent(
                "REPEAT_WIDTH", new java.math.BigDecimal("800"), 3, "TOTAL",
                List.of(new ProcessAiSourceAllocation("R1", 3)));
        ProcessAiAssignment assignment = new ProcessAiAssignment(List.of("R1"), "R1", List.of(),
                "REWIND", new ProcessAiRewindIntent("CHANGE_WIDTH", null, null, null, quantity),
                null, null, List.of(new ProcessAiEvidence("quantity", "800×3")));
        ProcessAiExtractionResult result = new ProcessAiExtractionResult("p1", "1.0",
                List.of(assignment), List.of(), List.of(), false, List.of());

        ProcessAiExtractionResult revised = service.apply(result,
                List.of(new ProcessAiCorrection("R1", "quantityScope", null, "PER_SOURCE", null)));

        assertThat(revised.assignments().getFirst().rewindIntent().quantityIntent())
                .satisfies(value -> {
                    assertThat(value.scope()).isEqualTo("PER_SOURCE");
                    assertThat(value.sourceAllocation()).isEmpty();
                });
    }

    @Test
    void correctionUpdatesCustomerPaperName() {
        ProcessAiExtractionResult revised = service.apply(extraction(), List.of(
                new ProcessAiCorrection("R1", "customerPaperName", null, "客户白卡", null, 0)));

        assertThat(revised.assignments().getFirst().customerSpecs()).singleElement()
                .satisfies(spec -> assertThat(spec.paperName()).isEqualTo("客户白卡"));
    }

    @Test
    void correctionUpdatesCustomerGramWeight() {
        ProcessAiExtractionResult revised = service.apply(extraction(), List.of(
                new ProcessAiCorrection("R1", "customerGramWeight", java.math.BigDecimal.valueOf(250),
                        null, null, 0)));

        assertThat(revised.assignments().getFirst().customerSpecs()).singleElement()
                .satisfies(spec -> assertThat(spec.gramWeight()).isEqualTo(250));
    }

    @Test
    void correctionUpdatesCustomerFinishWidth() {
        ProcessAiExtractionResult revised = service.apply(extraction(), List.of(
                new ProcessAiCorrection("R1", "customerFinishWidth", java.math.BigDecimal.valueOf(800),
                        null, "mm", 0)));

        assertThat(revised.assignments().getFirst().customerSpecs()).singleElement()
                .satisfies(spec -> assertThat(spec.finishWidth()).isEqualTo(800));
    }

    @Test
    void correctionUpdatesCustomerSpecOverrideReason() {
        ProcessAiExtractionResult revised = service.apply(extraction(), List.of(
                new ProcessAiCorrection("R1", "customerSpecOverrideReason", null,
                        "客户合同", null, 0)));

        assertThat(revised.assignments().getFirst().customerSpecs()).singleElement()
                .satisfies(spec -> assertThat(spec.overrideReason()).isEqualTo("客户合同"));
    }

    private ProcessAiExtractionResult extraction() {
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "REWIND",
                new ProcessAiRewindIntent("CHANGE_WIDTH", null, null, null), null, null,
                List.of(new ProcessAiEvidence("widthRule", "复卷")));
        return new ProcessAiExtractionResult("p1", "1.0", List.of(assignment),
                List.of(), List.of(), false, List.of());
    }
}
