package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import com.paper.mes.processorder.dto.RewindSourcePlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ProcessRoutePreviewerTest {

    private final ProcessRoutePreviewer previewer = new ProcessRoutePreviewer(mock(ProcessRouteCatalogPolicy.class));

    @Test
    void preview_whenSawThenRewindSelectedPiece_chargesSelectedPieceWeightOnly() {
        ProcessRoutePreviewDTO dto = sawThenRewindRoute();
        dto.getStages().get(1).setProcessWeight(new BigDecimal("9"));
        ProcessRoutePreviewVO preview = previewer.preview(roll(), dto);

        assertEquals(new BigDecimal("108"), preview.getTotalAmount());
        assertEquals(new BigDecimal("8"), preview.getStages().get(0).getStepAmount());
        assertEquals(new BigDecimal("100"), preview.getStages().get(1).getStepAmount());
        assertEquals(new BigDecimal("0.500"), preview.getStages().get(1).getProcessWeight());
        assertTrue(preview.getOutputs().get(0).getConsumedByNextStage());
        assertFalse(preview.getOutputs().get(1).getConsumedByNextStage());
    }

    @Test
    void preview_whenNextStageReferencesMissingOutput_throwsBusinessException() {
        ProcessRoutePreviewDTO dto = sawThenRewindRoute();
        dto.getStages().get(1).setInputOutputKeys(List.of("missing-output"));

        assertThrows(BusinessException.class, () -> previewer.preview(roll(), dto));
    }

    @Test
    void preview_whenNextStageHasNoInput_throwsBusinessException() {
        ProcessRoutePreviewDTO dto = sawThenRewindRoute();
        dto.getStages().get(1).setInputOutputKeys(List.of());

        assertThrows(BusinessException.class, () -> previewer.preview(roll(), dto));
    }

    @Test
    void preview_whenNextStageReferencesSameOutputTwice_throwsBusinessException() {
        ProcessRoutePreviewDTO dto = sawThenRewindRoute();
        dto.getStages().get(1).setInputOutputKeys(List.of("stage-output-a", "stage-output-a"));

        assertThrows(BusinessException.class, () -> previewer.preview(roll(), dto));
    }

    @Test
    void preview_whenLaterStageReferencesConsumedOutput_throwsBusinessException() {
        ProcessRoutePreviewDTO dto = sawThenRewindThenSawRoute();
        dto.getStages().get(2).setInputOutputKeys(List.of("stage-output-a"));

        assertThrows(BusinessException.class, () -> previewer.preview(roll(), dto));
    }

    @Test
    void preview_whenClientStageWeightsAreWrong_recomputesFromRouteGeometry() {
        ProcessRoutePreviewDTO dto = sawThenRewindRoute();
        dto.getStages().get(1).setOutputs(List.of(output("too-heavy", new BigDecimal("600.000"))));

        ProcessRoutePreviewVO preview = previewer.preview(roll(), dto);

        assertEquals(new BigDecimal("500"), preview.getOutputs().get(2).getEstimateWeight());
    }

    @Test
    void preview_whenStageOutputWidthExceedsSourceWidth_throwsBusinessException() {
        OriginalRoll source = roll();
        source.setOriginalWidth(1000);
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(source.getUuid());
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_SAW, "锯纸");
        stage.setKnifeCount(1);
        stage.setUnitPrice(new BigDecimal("8"));
        ProcessRoutePreviewDTO.RouteOutputDTO first = output("wide-a", new BigDecimal("500"));
        ProcessRoutePreviewDTO.RouteOutputDTO second = output("wide-b", new BigDecimal("500"));
        first.setFinishWidth(600);
        second.setFinishWidth(500);
        stage.setOutputs(List.of(first, second));
        dto.setStages(List.of(stage));

        assertThrows(BusinessException.class, () -> previewer.preview(source, dto));
    }

    @Test
    void preview_whenRewindLayoutWidthIsMissing_throwsBusinessException() {
        OriginalRoll source = roll();
        source.setOriginalWidth(1000);
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(source.getUuid());
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_REWIND, "复卷");
        stage.setPlan(rewindPlan(1, "REMAINDER", List.of(rewindSegment(1, null))));
        stage.setOutputs(List.of(sizedOutput("finish", 1000, new BigDecimal("1000"))));
        dto.setStages(List.of(stage));

        assertThrows(BusinessException.class, () -> previewer.preview(source, dto));
    }

    @Test
    void preview_lossPolicyAcceptsPlannedWidthLossAndExposesItOnStageLine() {
        OriginalRoll source = roll();
        source.setOriginalWidth(1000);
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(source.getUuid());
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_SAW, "锯纸");
        stage.setKnifeCount(1);
        stage.setUnitPrice(new BigDecimal("8"));
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setWidthDifferencePolicy("LOSS");
        stage.setPlan(plan);
        ProcessRoutePreviewDTO.RouteOutputDTO output = output("loss-finish", new BigDecimal("900"));
        output.setFinishWidth(900);
        stage.setOutputs(List.of(output));
        dto.setStages(List.of(stage));

        ProcessRoutePreviewVO preview = previewer.preview(source, dto);

        assertEquals("LOSS", preview.getStages().getFirst().getWidthDifferencePolicy());
        assertEquals(100, preview.getStages().getFirst().getPlannedLossWidth());
        assertEquals(new BigDecimal("100"), preview.getStages().getFirst().getPlannedLossWeight());
    }

    @Test
    void preview_remainderPolicyRequiresWidthDifferenceOutput() {
        OriginalRoll source = roll();
        source.setOriginalWidth(1000);
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(source.getUuid());
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_SAW, "锯纸");
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setWidthDifferencePolicy("REMAINDER");
        stage.setPlan(plan);
        ProcessRoutePreviewDTO.RouteOutputDTO output = output("finish", new BigDecimal("1000"));
        output.setFinishWidth(900);
        stage.setOutputs(List.of(output));
        dto.setStages(List.of(stage));

        assertThrows(BusinessException.class, () -> previewer.preview(source, dto));
    }

    @Test
    void preview_rewindLossTreatsMultipleSegmentsIndependently() {
        OriginalRoll source = roll();
        source.setOriginalWidth(1000);
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(source.getUuid());
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_REWIND, "复卷");
        stage.setPlan(rewindPlan(1, "LOSS", List.of(
                rewindSegment(1, 600), rewindSegment(1, 600))));
        stage.setOutputs(List.of(
                sizedOutput("finish-a", 600, new BigDecimal("300")),
                sizedOutput("finish-b", 600, new BigDecimal("300"))));
        dto.setStages(List.of(stage));

        ProcessRoutePreviewVO preview = previewer.preview(source, dto);

        assertEquals(new BigDecimal("400"), preview.getStages().getFirst().getPlannedLossWeight());
        assertEquals(400, preview.getStages().getFirst().getPlannedLossWidth());
    }

    @Test
    void preview_chainRejectsDifferentSourceWidths() {
        OriginalRoll source = roll();
        source.setOriginalWidth(2000);
        ProcessRoutePreviewDTO.RouteStageDTO first = stage(1, FeeCalculator.STEP_TYPE_SAW, "锯纸");
        ProcessRoutePreviewDTO.RouteOutputDTO firstOutput = output("source-a", new BigDecimal("500"));
        firstOutput.setFinishWidth(800);
        ProcessRoutePreviewDTO.RouteOutputDTO secondOutput = output("source-b", new BigDecimal("500"));
        secondOutput.setFinishWidth(600);
        first.setOutputs(List.of(firstOutput, secondOutput));

        ProcessRoutePreviewDTO.RouteStageDTO second = stage(2, FeeCalculator.STEP_TYPE_REWIND, "复卷");
        second.setInputOutputKeys(List.of("source-a", "source-b"));
        second.setPlan(rewindPlan(5, "ALLOCATE", List.of(
                rewindSegmentWithSource(1, 500, "source-a"),
                rewindSegmentWithSource(1, 500, "source-b"))));
        second.setOutputs(List.of(output("finish", new BigDecimal("1000"))));

        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(source.getUuid());
        dto.setStages(List.of(first, second));

        assertThrows(BusinessException.class, () -> previewer.preview(source, dto));
    }

    @Test
    void preview_modeFiveRequiresSegmentSourcesToMatchStageInputs() {
        ProcessRoutePreviewDTO dto = sawThenRewindRoute();
        ProcessRoutePreviewDTO.RouteStageDTO rewind = dto.getStages().get(1);
        rewind.setPlan(rewindPlan(5, "ALLOCATE", List.of(
                rewindSegmentWithSource(1, 1250, "unknown"))));

        assertThrows(BusinessException.class, () -> previewer.preview(roll(), dto));
    }

    @Test
    void preview_whenStageProducesDuplicateOutputKeys_throwsBusinessException() {
        ProcessRoutePreviewDTO dto = sawThenRewindRoute();
        dto.getStages().get(0).setOutputs(List.of(
                output("same-key", new BigDecimal("450.000")),
                output("same-key", new BigDecimal("550.000"))
        ));

        assertThrows(BusinessException.class, () -> previewer.preview(roll(), dto));
    }

    @Test
    void preview_whenOutputIsTrim_marksRemainOutput() {
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        dto.setStages(List.of(trimSawStage()));

        ProcessRoutePreviewVO preview = previewer.preview(roll(), dto);

        ProcessRoutePreviewVO.RouteOutputVO trim = preview.getOutputs().get(1);
        assertEquals("stage-trim", trim.getOutputKey());
        assertEquals(1, trim.getIsRemain());
        assertFalse(trim.getConsumedByNextStage());
    }

    @Test
    void preview_whenOriginalWeightIsUnknown_rejectsPlaceholderClosure() {
        OriginalRoll unknown = roll();
        unknown.setWeightStatus("UNKNOWN");
        unknown.setTotalWeight(new BigDecimal("1000"));
        unknown.setRollWeight(new BigDecimal("1000"));

        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(unknown.getUuid());
        dto.setStages(List.of(trimSawStage()));
        assertThrows(BusinessException.class, () -> previewer.preview(unknown, dto));
    }

    @Test
    void preview_recomputesSawWeightsByWidthAsWholeKilograms() {
        OriginalRoll source = roll();
        source.setOriginalWidth(2400);
        source.setActualWeight(new BigDecimal("1862"));
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_SAW, "锯纸");
        stage.setOutputs(List.of(
                sizedOutput("a", 800, new BigDecimal("621")),
                sizedOutput("b", 800, new BigDecimal("621")),
                sizedOutput("c", 800, new BigDecimal("621"))));
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(source.getUuid());
        dto.setStages(List.of(stage));

        ProcessRoutePreviewVO preview = previewer.preview(source, dto);

        assertEquals(List.of(new BigDecimal("621"), new BigDecimal("621"), new BigDecimal("620")),
                preview.getOutputs().stream().map(ProcessRoutePreviewVO.RouteOutputVO::getEstimateWeight).toList());
    }

    @Test
    void preview_sawAllocateLocksExplicitTrimWeightBeforeFinishes() {
        OriginalRoll source = roll();
        source.setOriginalWidth(1000);
        source.setActualWeight(new BigDecimal("1000"));
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setMainStepType(FeeCalculator.STEP_TYPE_SAW);
        plan.setWidthDifferencePolicy("ALLOCATE");
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_SAW, "锯纸");
        stage.setPlan(plan);
        stage.setOutputs(List.of(sizedOutput("finish", 600, BigDecimal.ZERO),
                trimOutputSized("trim", 100)));
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(source.getUuid());
        dto.setStages(List.of(stage));

        ProcessRoutePreviewVO preview = previewer.preview(source, dto);

        assertEquals(List.of(new BigDecimal("900"), new BigDecimal("100")),
                preview.getOutputs().stream().map(ProcessRoutePreviewVO.RouteOutputVO::getEstimateWeight).toList());
    }

    @Test
    void preview_rejectsMissingPlanOnLaterStage() {
        ProcessRoutePreviewDTO dto = sawThenRewindRoute();
        dto.getStages().get(1).setPlan(null);

        assertThrows(BusinessException.class, () -> previewer.preview(roll(), dto));
    }

    @Test
    void preview_rejectsEmptyRewindSegmentsOnLaterStage() {
        ProcessRoutePreviewDTO dto = sawThenRewindRoute();
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setRewindMode(1);
        plan.setSegments(List.of());
        dto.getStages().get(1).setPlan(plan);

        assertThrows(BusinessException.class, () -> previewer.preview(roll(), dto));
    }

    @Test
    void preview_modeFiveRejectsConsumptionThatDoesNotClosePerSource() {
        ProcessRoutePreviewDTO dto = sawThenRewindRoute();
        ProcessRoutePreviewDTO.RouteStageDTO rewind = dto.getStages().get(1);
        RewindSegmentPlanDTO segment = rewindSegment(1, 1250);
        RewindSourcePlanDTO first = new RewindSourcePlanDTO();
        first.setOriginalUuid("stage-output-a");
        first.setConsumeRatio(new BigDecimal("60"));
        RewindSourcePlanDTO second = new RewindSourcePlanDTO();
        second.setOriginalUuid("stage-output-b");
        second.setConsumeRatio(new BigDecimal("100"));
        segment.setSources(List.of(first, second));
        rewind.setPlan(rewindPlan(5, "ALLOCATE", List.of(segment)));

        assertThrows(BusinessException.class, () -> previewer.preview(roll(), dto));
    }

    @Test
    void preview_rejectsOutputCountDifferentFromPlannedLayout() {
        OriginalRoll source = roll();
        source.setOriginalWidth(2400);
        source.setActualWeight(new BigDecimal("1862"));
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setMainStepType(FeeCalculator.STEP_TYPE_SAW);
        plan.setWidthDifferencePolicy("LOSS");
        plan.setFinishSpecs(List.of(
                finishSpec(800), finishSpec(800), finishSpec(800)));
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_SAW, "锯纸");
        stage.setPlan(plan);
        stage.setOutputs(List.of(sizedOutput("a", 800, new BigDecimal("900")),
                sizedOutput("b", 800, new BigDecimal("900"))));
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(source.getUuid());
        dto.setStages(List.of(stage));

        assertThrows(BusinessException.class, () -> previewer.preview(source, dto));
    }

    @Test
    void preview_rejectsOutputWidthOrRemainFlagDifferentFromPlannedLayout() {
        OriginalRoll source = roll();
        source.setOriginalWidth(2400);
        source.setActualWeight(new BigDecimal("1862"));
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setWidthDifferencePolicy("ALLOCATE");
        plan.setFinishSpecs(List.of(finishSpec(800), finishSpec(800), finishSpec(800)));
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_SAW, "锯纸");
        stage.setPlan(plan);
        ProcessRoutePreviewDTO.RouteOutputDTO first = sizedOutput("a", 800, new BigDecimal("621"));
        first.setIsRemain(1);
        stage.setOutputs(List.of(first, sizedOutput("b", 800, new BigDecimal("621")),
                sizedOutput("c", 600, new BigDecimal("620"))));
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(source.getUuid());
        dto.setStages(List.of(stage));

        assertThrows(BusinessException.class, () -> previewer.preview(source, dto));
    }

    @Test
    void preview_modeFiveWeightsWidthDifferenceByConsumedSourceWeight() {
        OriginalRoll source = roll();
        ProcessRoutePreviewDTO.RouteStageDTO second = stage(2, FeeCalculator.STEP_TYPE_REWIND, "复卷");
        second.setInputOutputKeys(List.of("source-a", "source-b"));
        second.setUnitPrice(BigDecimal.ONE);
        second.setPlan(rewindPlan(5, "LOSS", List.of(
                rewindSegmentWithSource(1, 600, "source-a"),
                rewindSegmentWithSource(1, 900, "source-b"))));
        second.setOutputs(List.of(sizedOutput("finish-a", 600, new BigDecimal("600")),
                sizedOutput("finish-b", 900, new BigDecimal("400"))));

        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(source.getUuid());
        dto.setStages(List.of(second));

        ProcessStageOutput sourceA = existingOutput();
        sourceA.setUuid("output-a");
        sourceA.setStageLevel(1);
        sourceA.setFinishWidth(1000);
        sourceA.setEstimateWeight(new BigDecimal("800"));
        ProcessStageOutput sourceB = existingOutput();
        sourceB.setUuid("output-b");
        sourceB.setStageLevel(1);
        sourceB.setFinishWidth(1000);
        sourceB.setEstimateWeight(new BigDecimal("200"));
        ProcessRoutePreviewVO preview = previewer.previewFromExistingOutputs(source,
                Map.of("source-a", sourceA, "source-b", sourceB), dto);

        assertEquals(340, preview.getStages().getFirst().getPlannedLossWidth());
    }

    @Test
    void preview_rewindRepeatCountSplitsSegmentWeightAcrossRepeatedOutputs() {
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(2, FeeCalculator.STEP_TYPE_REWIND, "复卷");
        stage.setInputOutputKeys(List.of("source-a", "source-b"));
        RewindSegmentPlanDTO repeated = rewindSegmentWithSource(1, 500, "source-a");
        repeated.setRepeatCount(2);
        stage.setPlan(rewindPlan(5, "LOSS", List.of(
                repeated, rewindSegmentWithSource(1, 500, "source-b"))));
        stage.setOutputs(List.of(sizedOutput("finish-a", 500, BigDecimal.ZERO),
                sizedOutput("finish-b", 500, BigDecimal.ZERO),
                sizedOutput("finish-c", 500, BigDecimal.ZERO)));

        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        dto.setStages(List.of(stage));
        ProcessStageOutput sourceA = existingOutput();
        sourceA.setUuid("output-a");
        sourceA.setStageLevel(1);
        sourceA.setFinishWidth(1000);
        sourceA.setEstimateWeight(new BigDecimal("500"));
        ProcessStageOutput sourceB = existingOutput();
        sourceB.setUuid("output-b");
        sourceB.setStageLevel(1);
        sourceB.setFinishWidth(1000);
        sourceB.setEstimateWeight(new BigDecimal("500"));

        ProcessRoutePreviewVO preview = previewer.previewFromExistingOutputs(roll(),
                Map.of("source-a", sourceA, "source-b", sourceB), dto);

        assertEquals(List.of(new BigDecimal("125"), new BigDecimal("125"), new BigDecimal("250")),
                preview.getOutputs().stream().map(ProcessRoutePreviewVO.RouteOutputVO::getEstimateWeight).toList());
    }

    @Test
    void preview_rewindAllocateAssignsWidthDifferenceOnlyToFinishOutputs() {
        OriginalRoll source = roll();
        source.setOriginalWidth(1000);
        source.setActualWeight(new BigDecimal("1000"));
        RewindSegmentPlanDTO segment = rewindSegment(1, 600);
        RewindLayoutItemPlanDTO trim = new RewindLayoutItemPlanDTO();
        trim.setWidth(100);
        trim.setQuantity(1);
        trim.setItemType("TRIM");
        segment.setLayoutItems(List.of(segment.getLayoutItems().getFirst(), trim));
        ProcessPlanDTO plan = rewindPlan(1, "ALLOCATE", List.of(segment));
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_REWIND, "复卷");
        stage.setPlan(plan);
        ProcessRoutePreviewDTO.RouteOutputDTO finish = sizedOutput("finish", 600, BigDecimal.ZERO);
        ProcessRoutePreviewDTO.RouteOutputDTO remain = sizedOutput("remain", 100, BigDecimal.ZERO);
        remain.setIsRemain(1);
        stage.setOutputs(List.of(finish, remain));
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(source.getUuid());
        dto.setStages(List.of(stage));

        ProcessRoutePreviewVO preview = previewer.preview(source, dto);

        assertEquals(List.of(new BigDecimal("900"), new BigDecimal("100")),
                preview.getOutputs().stream().map(ProcessRoutePreviewVO.RouteOutputVO::getEstimateWeight).toList());
    }

    @Test
    void preview_rewindLossUsesWeightedFractionalDifferenceForWeightClosure() {
        OriginalRoll source = roll();
        source.setOriginalWidth(2400);
        source.setActualWeight(new BigDecimal("1862"));
        RewindSegmentPlanDTO first = rewindSegment(50, 2399);
        RewindSegmentPlanDTO second = rewindSegment(50, 2400);
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_REWIND, "复卷");
        stage.setPlan(rewindPlan(1, "LOSS", List.of(first, second)));
        stage.setOutputs(List.of(sizedOutput("first", 2399, BigDecimal.ZERO),
                sizedOutput("second", 2400, BigDecimal.ZERO)));
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(source.getUuid());
        dto.setStages(List.of(stage));

        ProcessRoutePreviewVO preview = previewer.preview(source, dto);

        assertEquals(new BigDecimal("1862"), preview.getOutputs().stream()
                .map(ProcessRoutePreviewVO.RouteOutputVO::getEstimateWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void preview_chainUsesMeasuredSourceWeightBeforeIntegerDisplayRounding() {
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(2, FeeCalculator.STEP_TYPE_REWIND, "复卷");
        stage.setInputOutputKeys(List.of("source-a", "source-b"));
        stage.setPlan(rewindPlan(5, "ALLOCATE", List.of(
                rewindSegmentWithSource(1, 500, "source-a"),
                rewindSegmentWithSource(1, 500, "source-b"))));
        stage.setOutputs(List.of(sizedOutput("finish-a", 500, BigDecimal.ZERO),
                sizedOutput("finish-b", 500, BigDecimal.ZERO)));
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        dto.setStages(List.of(stage));
        ProcessStageOutput sourceA = existingOutput();
        sourceA.setUuid("output-a");
        sourceA.setStageLevel(1);
        sourceA.setFinishWidth(1000);
        sourceA.setEstimateWeight(new BigDecimal("621"));
        sourceA.setActualWeight(new BigDecimal("620.6"));
        ProcessStageOutput sourceB = existingOutput();
        sourceB.setUuid("output-b");
        sourceB.setStageLevel(1);
        sourceB.setFinishWidth(1000);
        sourceB.setEstimateWeight(new BigDecimal("380"));
        sourceB.setActualWeight(new BigDecimal("379.6"));

        ProcessRoutePreviewVO preview = previewer.previewFromExistingOutputs(roll(),
                Map.of("source-a", sourceA, "source-b", sourceB), dto);

        assertEquals(new BigDecimal("1.000"), preview.getStages().getFirst().getProcessWeight());
    }

    @Test
    void preview_modeFiveUsesLegacyEmptyRatioForWidthLoss() {
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(2, FeeCalculator.STEP_TYPE_REWIND, "复卷");
        stage.setInputOutputKeys(List.of("source-a", "source-b"));
        RewindSegmentPlanDTO first = rewindSegmentWithSource(1, 500, "source-a");
        RewindSegmentPlanDTO second = rewindSegmentWithSource(1, 900, "source-b");
        second.getSources().getFirst().setConsumeRatio(null);
        stage.setPlan(rewindPlan(5, "LOSS", List.of(first, second)));
        stage.setOutputs(List.of(sizedOutput("finish-a", 500, BigDecimal.ZERO),
                sizedOutput("finish-b", 900, BigDecimal.ZERO)));
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        dto.setStages(List.of(stage));
        ProcessStageOutput sourceA = existingOutput();
        sourceA.setUuid("output-a");
        sourceA.setStageLevel(1);
        sourceA.setFinishWidth(1000);
        sourceA.setEstimateWeight(new BigDecimal("900"));
        ProcessStageOutput sourceB = existingOutput();
        sourceB.setUuid("output-b");
        sourceB.setStageLevel(1);
        sourceB.setFinishWidth(1000);
        sourceB.setEstimateWeight(new BigDecimal("100"));

        ProcessRoutePreviewVO preview = previewer.previewFromExistingOutputs(roll(),
                Map.of("source-a", sourceA, "source-b", sourceB), dto);

        assertEquals(460, preview.getStages().getFirst().getPlannedLossWidth());
        assertEquals(List.of(new BigDecimal("450"), new BigDecimal("90")),
                preview.getOutputs().stream()
                        .map(ProcessRoutePreviewVO.RouteOutputVO::getEstimateWeight).toList());
    }

    @Test
    void preview_modeFiveRejectsOutputSegmentWithZeroConsumedWeight() {
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(2, FeeCalculator.STEP_TYPE_REWIND, "复卷");
        stage.setInputOutputKeys(List.of("source-a", "source-b"));
        RewindSegmentPlanDTO zero = rewindSegmentWithSource(1, 500, "source-a");
        zero.getSources().getFirst().setConsumeRatio(BigDecimal.ZERO);
        RewindSegmentPlanDTO consumed = rewindSegmentWithSource(1, 500, "source-a");
        RewindSourcePlanDTO secondSource = new RewindSourcePlanDTO();
        secondSource.setOriginalUuid("source-b");
        secondSource.setConsumeRatio(new BigDecimal("100"));
        consumed.setSources(List.of(consumed.getSources().getFirst(), secondSource));
        stage.setPlan(rewindPlan(5, "ALLOCATE", List.of(zero, consumed)));
        stage.setOutputs(List.of(sizedOutput("finish-a", 500, BigDecimal.ZERO),
                sizedOutput("finish-b", 500, BigDecimal.ZERO)));
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        dto.setStages(List.of(stage));
        ProcessStageOutput sourceA = existingOutput();
        sourceA.setStageLevel(1);
        sourceA.setFinishWidth(1000);
        sourceA.setEstimateWeight(new BigDecimal("500"));
        ProcessStageOutput sourceB = existingOutput();
        sourceB.setStageLevel(1);
        sourceB.setFinishWidth(1000);
        sourceB.setEstimateWeight(new BigDecimal("500"));

        assertThrows(BusinessException.class, () -> previewer.previewFromExistingOutputs(roll(),
                Map.of("source-a", sourceA, "source-b", sourceB), dto));
    }

    @Test
    void preview_whenNextStageReferencesTrim_throwsBusinessException() {
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        dto.setStages(List.of(
                trimSawStage(),
                rewindStage("stage-trim")
        ));

        assertThrows(BusinessException.class, () -> previewer.preview(roll(), dto));
    }

    @Test
    void preview_whenRewindConsumesTwoOutputs_chargesCombinedWeight() {
        ProcessRoutePreviewVO preview = previewer.preview(roll(), sawThenRewindBothOutputsRoute());

        assertEquals(new BigDecimal("208"), preview.getTotalAmount());
        assertEquals(new BigDecimal("1.000"), preview.getStages().get(1).getProcessWeight());
        assertEquals(new BigDecimal("200"), preview.getStages().get(1).getStepAmount());
    }

    @Test
    void preview_whenThreeStageRoute_chargesEveryStageAndMarksMiddleOutputConsumed() {
        ProcessRoutePreviewDTO dto = sawThenRewindThenSawRoute();

        ProcessRoutePreviewVO preview = previewer.preview(roll(), dto);

        assertEquals(new BigDecimal("124"), preview.getTotalAmount());
        assertEquals(3, preview.getStages().size());
        assertEquals(new BigDecimal("16"), preview.getStages().get(2).getStepAmount());
        assertTrue(preview.getOutputs().get(0).getConsumedByNextStage());
        assertTrue(preview.getOutputs().get(2).getConsumedByNextStage());
        assertFalse(preview.getOutputs().get(3).getConsumedByNextStage());
    }

    @Test
    void previewFromExistingOutputs_whenAppendingRewind_usesSelectedOutputWeight() {
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        ProcessRoutePreviewDTO.RouteStageDTO stage = rewindStage(List.of("A0001"), "stage-output-next",
                new BigDecimal("720.000"));
        stage.setStageLevel(3);
        dto.setStages(List.of(stage));

        ProcessRoutePreviewVO preview = previewer.previewFromExistingOutputs(
                roll(), Map.of("A0001", existingOutput()), dto);

        assertEquals(new BigDecimal("0.720"), preview.getStages().get(0).getProcessWeight());
        assertEquals(new BigDecimal("144"), preview.getStages().get(0).getStepAmount());
    }

    @Test
    void previewFromExistingOutputs_whenSourceHasActualWeight_usesActualWeight() {
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        ProcessRoutePreviewDTO.RouteStageDTO stage = rewindStage(List.of("A0001"), "stage-output-next",
                new BigDecimal("620.000"));
        stage.setStageLevel(3);
        dto.setStages(List.of(stage));

        ProcessStageOutput source = existingOutput();
        source.setEstimateWeight(new BigDecimal("621.000"));
        source.setActualWeight(new BigDecimal("620.000"));
        ProcessRoutePreviewVO preview = previewer.previewFromExistingOutputs(
                roll(), Map.of("A0001", source), dto);

        assertEquals(new BigDecimal("0.620"), preview.getStages().get(0).getProcessWeight());
    }

    @Test
    void previewFromExistingOutputs_whenSourceWeightUnknown_rejectsChain() {
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        ProcessRoutePreviewDTO.RouteStageDTO stage = rewindStage(List.of("A0001"), "stage-output-next",
                new BigDecimal("0.000"));
        stage.setStageLevel(3);
        dto.setStages(List.of(stage));

        ProcessStageOutput source = existingOutput();
        source.setEstimateWeight(null);
        source.setActualWeight(null);

        assertThrows(BusinessException.class, () -> previewer.previewFromExistingOutputs(
                roll(), Map.of("A0001", source), dto));
    }

    @Test
    void preview_whenExpandedOutputCountIs500_accepts() {
        ProcessRoutePreviewDTO dto = routeWithOutputCounts(250, 250);

        ProcessRoutePreviewVO preview = previewer.preview(roll(), dto);

        assertEquals(500, preview.getOutputs().size());
    }

    @Test
    void preview_whenExpandedOutputCountIs501_rejectsBeforeExpansion() {
        ProcessRoutePreviewDTO dto = routeWithOutputCounts(250, 251);

        assertThrows(BusinessException.class, () -> previewer.preview(roll(), dto));
    }

    @Test
    void preview_whenOutputCountIsExtreme_rejectsBeforeExpansion() {
        ProcessRoutePreviewDTO dto = routeWithOutputCounts(Integer.MAX_VALUE);

        assertThrows(BusinessException.class, () -> previewer.preview(roll(), dto));
    }

    private ProcessRoutePreviewDTO sawThenRewindRoute() {
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        dto.setStages(List.of(
                sawStage(),
                rewindStage("stage-output-a")
        ));
        return dto;
    }

    private ProcessRoutePreviewDTO routeWithOutputCounts(int... counts) {
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_SAW, "锯纸");
        stage.setKnifeCount(1);
        stage.setUnitPrice(BigDecimal.ONE);
        stage.setOutputs(java.util.stream.IntStream.range(0, counts.length)
                .mapToObj(index -> outputWithCount("output-" + index, counts[index]))
                .toList());
        dto.setStages(List.of(stage));
        return dto;
    }

    private ProcessRoutePreviewDTO sawThenRewindBothOutputsRoute() {
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        dto.setStages(List.of(
                sawStage(),
                rewindStage(List.of("stage-output-a", "stage-output-b"), "stage-output-finish",
                        new BigDecimal("1000.000"))
        ));
        return dto;
    }

    private ProcessRoutePreviewDTO sawThenRewindThenSawRoute() {
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        dto.setStages(List.of(
                sawStage(),
                rewindStage("stage-output-a"),
                sawStageThree("stage-output-finish")
        ));
        return dto;
    }

    private ProcessRoutePreviewDTO.RouteStageDTO sawStage() {
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_SAW, "锯纸");
        stage.setKnifeCount(1);
        stage.setUnitPrice(new BigDecimal("8"));
        stage.setOutputs(List.of(
                output("stage-output-a", new BigDecimal("450.000")),
                output("stage-output-b", new BigDecimal("550.000"))
        ));
        return stage;
    }

    private ProcessRoutePreviewDTO.RouteStageDTO trimSawStage() {
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, FeeCalculator.STEP_TYPE_SAW, "锯纸");
        stage.setKnifeCount(1);
        stage.setUnitPrice(new BigDecimal("8"));
        stage.setOutputs(List.of(
                output("stage-output-a", new BigDecimal("900.000")),
                trimOutput("stage-trim", new BigDecimal("100.000"))
        ));
        return stage;
    }

    private ProcessRoutePreviewDTO.RouteStageDTO rewindStage(String outputKey) {
        return rewindStage(List.of(outputKey), "stage-output-finish", new BigDecimal("450.000"));
    }

    private ProcessRoutePreviewDTO.RouteStageDTO rewindStage(List<String> outputKeys, String finishKey) {
        return rewindStage(outputKeys, finishKey, new BigDecimal("450.000"));
    }

    private ProcessRoutePreviewDTO.RouteStageDTO rewindStage(List<String> outputKeys, String finishKey,
                                                              BigDecimal finishWeight) {
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(2, FeeCalculator.STEP_TYPE_REWIND, "复卷");
        stage.setInputOutputKeys(outputKeys);
        stage.setUnitPrice(new BigDecimal("200"));
        stage.setPlan(rewindPlan(1, "ALLOCATE", List.of(rewindSegment(1, 1250))));
        stage.setOutputs(List.of(output(finishKey, finishWeight)));
        return stage;
    }

    private ProcessRoutePreviewDTO.RouteStageDTO sawStageThree(String outputKey) {
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(3, FeeCalculator.STEP_TYPE_SAW, "锯纸");
        stage.setInputOutputKeys(List.of(outputKey));
        stage.setKnifeCount(2);
        stage.setUnitPrice(new BigDecimal("8"));
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setMainStepType(FeeCalculator.STEP_TYPE_SAW);
        plan.setWidthDifferencePolicy("ALLOCATE");
        plan.setFinishSpecs(List.of(finishSpec(625), finishSpec(625)));
        stage.setPlan(plan);
        ProcessRoutePreviewDTO.RouteOutputDTO first = output("stage-output-c", new BigDecimal("225.000"));
        ProcessRoutePreviewDTO.RouteOutputDTO second = output("stage-output-d", new BigDecimal("225.000"));
        first.setFinishWidth(625);
        second.setFinishWidth(625);
        stage.setOutputs(List.of(first, second));
        return stage;
    }

    private ProcessRoutePreviewDTO.RouteStageDTO stage(int level, int stepType, String name) {
        ProcessRoutePreviewDTO.RouteStageDTO stage = new ProcessRoutePreviewDTO.RouteStageDTO();
        stage.setStageLevel(level);
        stage.setStepType(stepType);
        stage.setStepName(name);
        return stage;
    }

    private ProcessPlanDTO rewindPlan(int mode, String policy,
                                      List<RewindSegmentPlanDTO> segments) {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setMainStepType(FeeCalculator.STEP_TYPE_REWIND);
        plan.setRewindMode(mode);
        plan.setWidthDifferencePolicy(policy);
        plan.setSegments(segments);
        return plan;
    }

    private RewindSegmentPlanDTO rewindSegment(int ratio, Integer width) {
        RewindSegmentPlanDTO segment = new RewindSegmentPlanDTO();
        segment.setSegmentRatio(new BigDecimal(ratio));
        RewindLayoutItemPlanDTO item = new RewindLayoutItemPlanDTO();
        item.setWidth(width);
        item.setQuantity(1);
        item.setItemType("FINISH");
        segment.setLayoutItems(List.of(item));
        return segment;
    }

    private RewindSegmentPlanDTO rewindSegmentWithSource(int ratio, int width, String sourceKey) {
        RewindSegmentPlanDTO segment = rewindSegment(ratio, width);
        RewindSourcePlanDTO source = new RewindSourcePlanDTO();
        source.setOriginalUuid(sourceKey);
        source.setConsumeRatio(new BigDecimal("100"));
        segment.setSources(List.of(source));
        return segment;
    }

    private ProcessRoutePreviewDTO.RouteOutputDTO output(String key, BigDecimal weight) {
        ProcessRoutePreviewDTO.RouteOutputDTO output = new ProcessRoutePreviewDTO.RouteOutputDTO();
        output.setOutputKey(key);
        output.setPaperName("牛卡纸");
        output.setGramWeight(450);
        output.setFinishWidth(1250);
        output.setEstimateWeight(weight);
        return output;
    }

    private ProcessRoutePreviewDTO.RouteOutputDTO sizedOutput(String key, int width, BigDecimal weight) {
        ProcessRoutePreviewDTO.RouteOutputDTO output = output(key, weight);
        output.setFinishWidth(width);
        return output;
    }

    private com.paper.mes.processorder.dto.FinishConfigSpecDTO finishSpec(int width) {
        com.paper.mes.processorder.dto.FinishConfigSpecDTO spec =
                new com.paper.mes.processorder.dto.FinishConfigSpecDTO();
        spec.setItemType("FINISH");
        spec.setFinishWidth(width);
        spec.setCount(1);
        return spec;
    }

    private ProcessRoutePreviewDTO.RouteOutputDTO trimOutput(String key, BigDecimal weight) {
        ProcessRoutePreviewDTO.RouteOutputDTO output = output(key, weight);
        output.setIsRemain(1);
        output.setRemark("修边/余料");
        return output;
    }

    private ProcessRoutePreviewDTO.RouteOutputDTO trimOutputSized(String key, int width) {
        ProcessRoutePreviewDTO.RouteOutputDTO output = trimOutput(key, BigDecimal.ZERO);
        output.setFinishWidth(width);
        return output;
    }

    private ProcessRoutePreviewDTO.RouteOutputDTO outputWithCount(String key, int count) {
        ProcessRoutePreviewDTO.RouteOutputDTO output = output(key, new BigDecimal("2"));
        output.setCount(count);
        return output;
    }

    private OriginalRoll roll() {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setPaperName("牛卡纸");
        roll.setGramWeight(450);
        roll.setRollWeight(new BigDecimal("1000"));
        roll.setPieceNum(1);
        return roll;
    }

    private ProcessStageOutput existingOutput() {
        ProcessStageOutput output = new ProcessStageOutput();
        output.setUuid("output-1");
        output.setOutputNo("S2-F1");
        output.setStageLevel(2);
        output.setOutputSort(1);
        output.setPaperName("牛卡纸");
        output.setGramWeight(450);
        output.setFinishWidth(1250);
        output.setEstimateWeight(new BigDecimal("720.000"));
        return output;
    }
}
