package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.context.ProcessAiBaselinePlan;
import com.paper.mes.ai.process.context.ProcessAiReviewBaseline;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiDiameterRule;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiMeasurement;
import com.paper.mes.ai.process.intent.ProcessAiRewindIntent;
import com.paper.mes.ai.process.intent.ProcessAiSawIntent;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.service.ProcessOrderDraftService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiPlanCompilationServiceTest {

    @Test
    void compile_whenExistingPreviewIsReady_marksCandidateEligible() {
        ProcessOrderDraftService drafts = mock(ProcessOrderDraftService.class);
        PlanPreviewVO preview = new PlanPreviewVO();
        preview.setReady(true);
        when(drafts.previewProcessPlan(eq("order-1"), eq("roll-1"), any(), eq(3)))
                .thenReturn(preview);
        ProcessAiPlanCompilationService service = new ProcessAiPlanCompilationService(
                compiler(), drafts, new ProcessAiPackagingCandidateCompiler(),
                new ProcessAiNewPlanCompletenessGuard());

        ProcessAiCompilationResult result = service.compile(extraction(), context());

        assertThat(result.eligible()).isTrue();
        assertThat(result.plans()).hasSize(1);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void compile_whenExtractionNeedsClarification_doesNotPreview() {
        ProcessOrderDraftService drafts = mock(ProcessOrderDraftService.class);
        ProcessAiPlanCompilationService service = new ProcessAiPlanCompilationService(
                compiler(), drafts, new ProcessAiPackagingCandidateCompiler(),
                new ProcessAiNewPlanCompletenessGuard());
        ProcessAiExtractionResult blocked = new ProcessAiExtractionResult(
                "parse-1", "1.0", extraction().assignments(), List.of(), List.of(),
                true, List.of("请确认母卷"));

        ProcessAiCompilationResult result = service.compile(blocked, context());

        assertThat(result.eligible()).isFalse();
        assertThat(result.plans()).isEmpty();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void compile_whenOwnerHasRouteDraft_rejectsSinglePlanBeforePreview() {
        ProcessOrderDraftService drafts = mock(ProcessOrderDraftService.class);
        ProcessAiPlanCompilationService service = new ProcessAiPlanCompilationService(
                compiler(), drafts, new ProcessAiPackagingCandidateCompiler(),
                new ProcessAiNewPlanCompletenessGuard());
        ProcessAiOrderContext base = context();
        ProcessAiReviewBaseline baseline = new ProcessAiReviewBaseline(null, List.of(
                new ProcessAiBaselinePlan("R1", "roll-1", 1, 2, true, null)));
        ProcessAiOrderContext routeContext = new ProcessAiOrderContext(
                base.orderUuid(), base.draftVersion(), base.remarkLong(), base.rolls(), baseline);

        ProcessAiCompilationResult result = service.compile(extraction(), routeContext);

        assertThat(result.eligible()).isFalse();
        assertThat(result.errors()).anyMatch(error -> error.contains("链式工艺"));
        verify(drafts, never()).previewProcessPlan(any(), any(), any(), any());
    }

    @Test
    void compile_defersSawWidthOverflowToExistingDraftPreview() {
        ProcessOrderDraftService drafts = mock(ProcessOrderDraftService.class);
        PlanPreviewVO preview = new PlanPreviewVO();
        preview.setReady(false);
        preview.setErrors(List.of("现有预览：成品门幅合计超过母卷门幅"));
        when(drafts.previewProcessPlan(eq("order-1"), eq("roll-1"), any(), eq(3)))
                .thenReturn(preview);
        ProcessAiPlanCompilationService service = new ProcessAiPlanCompilationService(
                compiler(), drafts, new ProcessAiPackagingCandidateCompiler(),
                new ProcessAiNewPlanCompletenessGuard());
        ProcessAiSawIntent saw = new ProcessAiSawIntent(
                "EXPLICIT_WIDTHS", null, List.of(900, 900, 900), "mm");
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW", null, saw, null, List.of());
        ProcessAiExtractionResult extraction = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment),
                List.of(), List.of(), false, List.of());

        ProcessAiCompilationResult result = service.compile(extraction, context());

        verify(drafts).previewProcessPlan(eq("order-1"), eq("roll-1"), any(), eq(3));
        assertThat(result.errors()).containsExactly(
                "R1: 现有预览：成品门幅合计超过母卷门幅");
    }

    @Test
    void compile_newRewindWithoutWidthRule_isRejectedBeforeConfirmation() {
        ProcessOrderDraftService drafts = mock(ProcessOrderDraftService.class);
        PlanPreviewVO preview = new PlanPreviewVO();
        preview.setReady(true);
        when(drafts.previewProcessPlan(eq("order-1"), eq("roll-1"), any(), eq(3)))
                .thenReturn(preview);
        ProcessAiPlanCompilationService service = new ProcessAiPlanCompilationService(
                compiler(), drafts, new ProcessAiPackagingCandidateCompiler(),
                new ProcessAiNewPlanCompletenessGuard());
        ProcessAiDiameterRule diameter = new ProcessAiDiameterRule(
                "EXPLICIT", 1, List.of(new BigDecimal("100")),
                new ProcessAiMeasurement(new BigDecimal("1200"), "mm", "DEFAULT"));
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "REWIND",
                new ProcessAiRewindIntent(
                        "CHANGE_DIAMETER", diameter,
                        new ProcessAiMeasurement(new BigDecimal("3"), "inch", "DEFAULT"), null),
                null, null, List.of());
        ProcessAiExtractionResult extraction = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment),
                List.of(), List.of(), false, List.of());

        ProcessAiCompilationResult result = service.compile(extraction, context());

        assertThat(result.eligible()).isFalse();
        assertThat(result.errors()).containsExactly("R1: 请明确成品门幅，或明确保持母卷门幅");
    }

    private ProcessAiPlanCompiler compiler() {
        AiProperties properties = new AiProperties();
        ProcessAiRewindPlanCompiler rewind = new ProcessAiRewindPlanCompiler(
                new ProcessAiDiameterStorageConverter(),
                new ProcessAiRewindSegmentCompiler(), properties);
        return new ProcessAiPlanCompiler(rewind, new ProcessAiSawPlanCompiler(),
                mock(ProcessAiPlanMachineResolver.class));
    }

    private ProcessAiExtractionResult extraction() {
        ProcessAiSawIntent saw = new ProcessAiSawIntent("CUTS", 2, null, "mm");
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW",
                null, saw, null, List.of());
        return new ProcessAiExtractionResult("parse-1", "1.0",
                List.of(assignment), List.of(), List.of(), false, List.of());
    }

    private ProcessAiOrderContext context() {
        ProcessAiRollContext roll = new ProcessAiRollContext(
                "R1", "roll-1", 1, "白卡纸", 80, 2000, 48, 6,
                new BigDecimal("800"), 1, 1, 2);
        return new ProcessAiOrderContext("order-1", 3, null, List.of(roll));
    }
}
