package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnSitePlanPreviewerTest {

    private final OnSitePlanPreviewer previewer = new OnSitePlanPreviewer();

    @Test
    void preview_countsExpectedFinishRowsWithoutWidth() {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(2);
        plan.setMainStepType(1);
        plan.setFinishSpecs(List.of(spec(3)));

        PlanPreviewVO preview = previewer.preview(plan, "roll-1");

        assertTrue(preview.isReady());
        assertEquals(3, preview.getFinishCount());
        assertEquals(3, preview.getFinishes().size());
        assertEquals(0, preview.getFinishes().getFirst().getFinishWidth());
    }

    private FinishConfigSpecDTO spec(int count) {
        FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
        spec.setItemType("FINISH");
        spec.setFinishWidth(0);
        spec.setCount(count);
        return spec;
    }
}
