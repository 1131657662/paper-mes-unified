package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.intent.ProcessAiAncillaryRequirements;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiPackagingRequirement;
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

class ProcessAiRollConfigurationCompilationTest {

    @Test
    void compile_serviceOnlyConfiguresTheRollAndSavesStripSortingWithoutAMainPlan() {
        ProcessOrderDraftService drafts = mock(ProcessOrderDraftService.class);
        ProcessAiPlanCompilationService service = service(drafts);
        ProcessAiPackagingRequirement packaging = new ProcessAiPackagingRequirement(
                "STRIP_SORT", "[金额]", "PIECE", "STANDARD", true);
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SERVICE_ONLY", "SERVICE_ONLY", null, null,
                new ProcessAiAncillaryRequirements(null, packaging), List.of(), List.of());

        ProcessAiCompilationResult result = service.compile(extraction(assignment), context(),
                List.of(new com.paper.mes.ai.process.security.ProcessTextRedactor.ExtractedCharge(
                        new BigDecimal("20"), "PIECE")));

        assertThat(result.eligible()).isTrue();
        assertThat(result.plans()).isEmpty();
        assertThat(result.rollConfigurations()).containsExactly(
                new ProcessAiRollConfiguration("R1", List.of("roll-1"), 4, null));
        assertThat(result.packagingCandidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.stepType()).isEqualTo(3);
            assertThat(candidate.serviceQuantity()).isNull();
            assertThat(candidate.billingMode()).isEqualTo(1);
        });
        verify(drafts, never()).previewProcessPlan(any(), any(), any(), any());
    }

    @Test
    void compile_directShipConfiguresTheRollWithoutMainOrServiceSteps() {
        ProcessOrderDraftService drafts = mock(ProcessOrderDraftService.class);
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "DIRECT_SHIP", "DIRECT_SHIP", null, null,
                null, List.of(), List.of());

        ProcessAiCompilationResult result = service(drafts).compile(extraction(assignment), context());

        assertThat(result.eligible()).isTrue();
        assertThat(result.plans()).isEmpty();
        assertThat(result.packagingCandidates()).isEmpty();
        assertThat(result.rollConfigurations()).containsExactly(
                new ProcessAiRollConfiguration("R1", List.of("roll-1"), 3, null));
        verify(drafts, never()).previewProcessPlan(any(), any(), any(), any());
    }

    @Test
    void compile_onSiteSawUsesOnSiteModeForTheMainPlanAndRollConfiguration() {
        ProcessOrderDraftService drafts = mock(ProcessOrderDraftService.class);
        PlanPreviewVO preview = new PlanPreviewVO();
        preview.setReady(true);
        when(drafts.previewProcessPlan(eq("order-1"), eq("roll-1"), any(), eq(3)))
                .thenReturn(preview);
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW", "ON_SITE", null,
                new ProcessAiSawIntent("CUTS", 2, null, "mm"), null, List.of(), List.of());

        ProcessAiCompilationResult result = service(drafts).compile(extraction(assignment), context());

        assertThat(result.eligible()).isTrue();
        assertThat(result.rollConfigurations()).containsExactly(
                new ProcessAiRollConfiguration("R1", List.of("roll-1"), 2, 1));
        assertThat(result.plans()).singleElement().satisfies(plan ->
                assertThat(plan.plan().getProcessMode()).isEqualTo(2));
    }

    private ProcessAiPlanCompilationService service(ProcessOrderDraftService drafts) {
        AiProperties properties = new AiProperties();
        ProcessAiPlanCompiler compiler = new ProcessAiPlanCompiler(
                new ProcessAiRewindPlanCompiler(new ProcessAiDiameterStorageConverter(),
                        new ProcessAiRewindSegmentCompiler(), properties),
                new ProcessAiSawPlanCompiler(), mock(ProcessAiPlanMachineResolver.class));
        return new ProcessAiPlanCompilationService(compiler, drafts,
                new ProcessAiPackagingCandidateCompiler(), new ProcessAiNewPlanCompletenessGuard());
    }

    private ProcessAiExtractionResult extraction(ProcessAiAssignment assignment) {
        return new ProcessAiExtractionResult("parse-1", "1.0", List.of(assignment),
                List.of(), List.of(), false, List.of());
    }

    private ProcessAiOrderContext context() {
        ProcessAiRollContext roll = new ProcessAiRollContext(
                "R1", "roll-1", 1, "白卡纸", 80, 2000, 48, 6,
                new BigDecimal("800"), 1, 1, 2);
        return new ProcessAiOrderContext("order-1", 3, null, List.of(roll));
    }
}
