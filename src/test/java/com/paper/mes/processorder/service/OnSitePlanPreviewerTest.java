package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnSitePlanPreviewerTest {

    private final OnSitePlanPreviewer previewer = new OnSitePlanPreviewer();

    @Test
    void preview_withoutExpectedOutputs_isReadyForBackRecordDrivenProduction() {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(2);
        plan.setMainStepType(1);
        plan.setFinishSpecs(List.of());

        PlanPreviewVO preview = previewer.preview(plan, "roll-1");

        assertTrue(preview.isReady());
        assertEquals(0, preview.getFinishCount());
        assertTrue(preview.getFinishes().isEmpty());
        assertEquals("现场定尺不预生成成品号，实际成品和切边在回录时录入", preview.getSummary());
    }
}
