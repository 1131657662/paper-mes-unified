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

        assertEquals(2, finishes.size());
        assertEquals(1, finishes.getFirst().getCount());
        assertEquals(new BigDecimal("475.000"), finishes.getFirst().getEstimateWeight());
        assertEquals(new BigDecimal("475.000"), finishes.get(1).getEstimateWeight());
    }

    private ProcessPlanDTO plan(List<FinishConfigSpecDTO> specs) {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setMainStepType(1);
        plan.setSpareCount(0);
        plan.setFinishSpecs(specs);
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
        roll.setOriginalWidth(width);
        roll.setRollWeight(new BigDecimal(weight));
        roll.setPieceNum(1);
        return roll;
    }
}
