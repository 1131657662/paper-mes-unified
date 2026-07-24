package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SawPlanPreviewerTest {

    private final SawPlanPreviewer previewer = new SawPlanPreviewer();

    @Test
    void preview_withTrimRows_excludesTrimFromFinishCount() {
        PlanPreviewVO preview = previewer.preview(plan(List.of(
                spec("FINISH", 950, 2),
                spec("TRIM", 100, 1)
        )), roll(2000, "1000"));

        assertTrue(preview.isReady());
        assertEquals(2, preview.getFinishCount());
        assertEquals(1, preview.getTrimCount());
        assertEquals(2, preview.getFinishes().size());
        assertEquals(new BigDecimal("50.000"), preview.getTotalTrimWeight());
        assertTrue(preview.getSummary().contains("刀数 2"));
    }

    @Test
    void preview_whenWidthOverflow_returnsError() {
        PlanPreviewVO preview = previewer.preview(plan(List.of(
                spec("FINISH", 1000, 2),
                spec("TRIM", 100, 1)
        )), roll(2000, "1000"));

        assertFalse(preview.isReady());
        assertEquals(1, preview.getErrors().size());
        assertTrue(preview.getErrors().getFirst().contains("不能超过母卷门幅"));
    }

    @Test
    void finishSpecs_filtersTrimRows() {
        List<FinishConfigSpecDTO> finishes = previewer.finishSpecs(List.of(
                spec("FINISH", 800, 1),
                spec("TRIM", 50, 1)
        ));

        assertEquals(1, finishes.size());
        assertEquals(800, finishes.getFirst().getFinishWidth());
    }

    @Test
    void saveSpecs_expandsFinishRowsWithEstimateWeight() {
        List<FinishConfigSpecDTO> finishes = previewer.saveSpecs(List.of(
                spec("FINISH", 950, 2),
                spec("TRIM", 100, 1)
        ), roll(2000, "1000"));

        assertEquals(3, finishes.size());
        assertEquals(1, finishes.getFirst().getCount());
        assertEquals(new BigDecimal("475.000"), finishes.getFirst().getEstimateWeight());
        assertEquals(new BigDecimal("475.000"), finishes.get(1).getEstimateWeight());
        assertEquals("TRIM", finishes.get(2).getItemType());
        assertEquals(100, finishes.get(2).getFinishWidth());
        assertEquals(new BigDecimal("50.000"), finishes.get(2).getEstimateWeight());
    }

    @Test
    void preview_withoutTrimRows_usesRemainingWidthAsTrim() {
        PlanPreviewVO preview = previewer.preview(planWithPolicy(List.of(
                spec("FINISH", 950, 2)
        ), "REMAINDER"), roll(2000, "1000"));

        assertTrue(preview.isReady());
        assertEquals(2, preview.getFinishCount());
        assertEquals(1, preview.getTrimCount());
        assertEquals(new BigDecimal("50.000"), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("950.000"), preview.getTotalEstimateWeight());
        assertEquals(new BigDecimal("475.000"), preview.getFinishes().getFirst().getEstimateWeight());
        assertTrue(preview.getSummary().contains("100mm"));
    }

    @Test
    void preview_allocate_counts_theoretical_difference_once_in_finish_weight() {
        PlanPreviewVO preview = previewer.preview(planWithPolicy(List.of(
                spec("FINISH", 1175, 2)
        ), "ALLOCATE"), roll(2353, "2285"));

        assertEquals(new BigDecimal("2285.000"), preview.getTotalEstimateWeight());
        assertEquals(new BigDecimal("0.000"), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("2.913"), preview.getWidthDifferenceWeight());
        assertEquals(new BigDecimal("0"), preview.getCalculatedLossWeight());
        assertTrue(preview.getSummary().contains("分摊入成品"));
    }

    @Test
    void preview_loss_keeps_difference_as_planned_loss() {
        PlanPreviewVO preview = previewer.preview(planWithPolicy(List.of(
                spec("FINISH", 1175, 2)
        ), "LOSS"), roll(2353, "2285"));

        assertEquals(new BigDecimal("2282.087"), preview.getTotalEstimateWeight());
        assertEquals(new BigDecimal("2.913"), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("2.913"), preview.getCalculatedLossWeight());
    }

    @Test
    void preview_remainder_keeps_difference_as_inventory_remainder() {
        PlanPreviewVO preview = previewer.preview(planWithPolicy(List.of(
                spec("FINISH", 1175, 2)
        ), "REMAINDER"), roll(2353, "2285"));

        assertEquals(1, preview.getTrimCount());
        assertEquals(new BigDecimal("2282.087"), preview.getTotalEstimateWeight());
        assertEquals(new BigDecimal("2.913"), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("2.913"), preview.getWidthDifferenceWeight());
    }

    @Test
    void saveSpecs_withoutTrimRows_appendsImplicitTrimSpec() {
        List<FinishConfigSpecDTO> finishes = previewer.saveSpecs(List.of(
                spec("FINISH", 950, 2)
        ), roll(2000, "1000"));

        assertEquals(3, finishes.size());
        assertEquals("TRIM", finishes.get(2).getItemType());
        assertEquals(100, finishes.get(2).getFinishWidth());
        assertEquals(new BigDecimal("50.000"), finishes.get(2).getEstimateWeight());
    }

    @Test
    void saveSpecs_whenOnSite_doesNotInferTrimOrEstimateWeight() {
        OriginalRoll roll = roll(2000, "1000");
        roll.setProcessMode(2);

        List<FinishConfigSpecDTO> finishes = previewer.saveSpecs(List.of(
                spec("FINISH", 0, 2)
        ), roll);

        assertEquals(2, finishes.size());
        assertEquals("FINISH", finishes.getFirst().getItemType());
        assertEquals(new BigDecimal("0.000"), finishes.getFirst().getEstimateWeight());
    }

    private ProcessPlanDTO plan(List<FinishConfigSpecDTO> specs) {
        return planWithPolicy(specs, null);
    }

    private ProcessPlanDTO planWithPolicy(List<FinishConfigSpecDTO> specs, String policy) {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setMainStepType(1);
        plan.setSpareCount(0);
        plan.setFinishSpecs(specs);
        plan.setWidthDifferencePolicy(policy);
        return plan;
    }

    private FinishConfigSpecDTO spec(String itemType, int width, int count) {
        FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
        spec.setItemType(itemType);
        spec.setFinishWidth(width);
        spec.setCount(count);
        return spec;
    }

    private OriginalRoll roll(int width, String weight) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setProcessMode(1);
        roll.setOriginalWidth(width);
        roll.setRollWeight(new BigDecimal(weight));
        roll.setPieceNum(1);
        return roll;
    }
}
