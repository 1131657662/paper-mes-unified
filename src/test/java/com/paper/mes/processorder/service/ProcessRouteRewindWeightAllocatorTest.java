package com.paper.mes.processorder.service;

import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessRouteRewindWeightAllocatorTest {

    @Test
    void multipleTrimOutputsShareTrimBudgetAndRemainClosed() {
        ProcessRoutePreviewDTO.RouteStageDTO stage = new ProcessRoutePreviewDTO.RouteStageDTO();
        stage.setStepType(FeeCalculator.STEP_TYPE_REWIND);
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setRewindMode(1);
        plan.setWidthDifferencePolicy("ALLOCATE");
        RewindSegmentPlanDTO segment = new RewindSegmentPlanDTO();
        segment.setSegmentRatio(BigDecimal.ONE);
        segment.setLayoutItems(List.of(item(600, "FINISH"), item(100, "TRIM")));
        plan.setSegments(List.of(segment));
        stage.setPlan(plan);

        List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs = List.of(
                output("finish", 600, false), output("trim-a", 50, true), output("trim-b", 50, true));

        List<BigDecimal> weights = ProcessRouteRewindWeightAllocator.allocate(
                new BigDecimal("1000"), stage, outputs, 1000, Map.of());

        assertEquals(List.of(new BigDecimal("900"), new BigDecimal("50"), new BigDecimal("50")), weights);
        assertEquals(new BigDecimal("1000"), weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private RewindLayoutItemPlanDTO item(int width, String type) {
        RewindLayoutItemPlanDTO item = new RewindLayoutItemPlanDTO();
        item.setWidth(width);
        item.setQuantity(1);
        item.setItemType(type);
        return item;
    }

    private ProcessRoutePreviewDTO.RouteOutputDTO output(String key, int width, boolean trim) {
        ProcessRoutePreviewDTO.RouteOutputDTO output = new ProcessRoutePreviewDTO.RouteOutputDTO();
        output.setOutputKey(key);
        output.setFinishWidth(width);
        output.setIsRemain(trim ? 1 : 0);
        return output;
    }
}
