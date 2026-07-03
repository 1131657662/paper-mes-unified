package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
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

class ProcessRoutePreviewerTest {

    private final ProcessRoutePreviewer previewer = new ProcessRoutePreviewer();

    @Test
    void preview_whenSawThenRewindSelectedPiece_chargesSelectedPieceWeightOnly() {
        ProcessRoutePreviewVO preview = previewer.preview(roll(), sawThenRewindRoute());

        assertEquals(new BigDecimal("98"), preview.getTotalAmount());
        assertEquals(new BigDecimal("8"), preview.getStages().get(0).getStepAmount());
        assertEquals(new BigDecimal("90"), preview.getStages().get(1).getStepAmount());
        assertEquals(new BigDecimal("0.450"), preview.getStages().get(1).getProcessWeight());
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

        assertEquals(new BigDecimal("114"), preview.getTotalAmount());
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
        dto.setStages(List.of(rewindStage(List.of("A0001"), "stage-output-next")));

        ProcessRoutePreviewVO preview = previewer.previewFromExistingOutputs(
                roll(), Map.of("A0001", existingOutput()), dto);

        assertEquals(new BigDecimal("0.720"), preview.getStages().get(0).getProcessWeight());
        assertEquals(new BigDecimal("144"), preview.getStages().get(0).getStepAmount());
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

    private ProcessRoutePreviewDTO sawThenRewindBothOutputsRoute() {
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        dto.setStages(List.of(
                sawStage(),
                rewindStage(List.of("stage-output-a", "stage-output-b"), "stage-output-finish")
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

    private ProcessRoutePreviewDTO.RouteStageDTO rewindStage(String outputKey) {
        return rewindStage(List.of(outputKey), "stage-output-finish");
    }

    private ProcessRoutePreviewDTO.RouteStageDTO rewindStage(List<String> outputKeys, String finishKey) {
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(2, FeeCalculator.STEP_TYPE_REWIND, "复卷");
        stage.setInputOutputKeys(outputKeys);
        stage.setUnitPrice(new BigDecimal("200"));
        stage.setOutputs(List.of(output(finishKey, new BigDecimal("450.000"))));
        return stage;
    }

    private ProcessRoutePreviewDTO.RouteStageDTO sawStageThree(String outputKey) {
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(3, FeeCalculator.STEP_TYPE_SAW, "锯纸");
        stage.setInputOutputKeys(List.of(outputKey));
        stage.setKnifeCount(2);
        stage.setUnitPrice(new BigDecimal("8"));
        stage.setOutputs(List.of(
                output("stage-output-c", new BigDecimal("225.000")),
                output("stage-output-d", new BigDecimal("225.000"))
        ));
        return stage;
    }

    private ProcessRoutePreviewDTO.RouteStageDTO stage(int level, int stepType, String name) {
        ProcessRoutePreviewDTO.RouteStageDTO stage = new ProcessRoutePreviewDTO.RouteStageDTO();
        stage.setStageLevel(level);
        stage.setStepType(stepType);
        stage.setStepName(name);
        return stage;
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
