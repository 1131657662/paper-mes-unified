package com.paper.mes.processorder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessParam;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.mapper.ProcessParamMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProcessRouteParamWriterTest {

    @Test
    void modeFourRepeatsPersistEveryLayerWithCanonicalPieceEstimate() {
        ProcessParamMapper mapper = mock(ProcessParamMapper.class);
        ProcessRouteParamWriter writer = new ProcessRouteParamWriter(mapper, new ObjectMapper());
        ProcessRouteContext context = context();
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(4);
        RewindSegmentPlanDTO segment = new RewindSegmentPlanDTO();
        segment.setRepeatCount(2);
        RewindLayoutItemPlanDTO item = new RewindLayoutItemPlanDTO();
        item.setWidth(800);
        item.setQuantity(1);
        item.setLayers(List.of(layer(30, 3), layer(24, 3)));
        segment.setLayoutItems(List.of(item));
        stage.getPlan().setSegments(List.of(segment));

        Map<String, ProcessStageOutput> outputs = new LinkedHashMap<>();
        outputs.put("a", output("step-1", 1, "100"));
        outputs.put("b", output("step-1", 2, "101"));
        writer.write(context, dto(stage), outputs);

        var captor = org.mockito.ArgumentCaptor.forClass(ProcessParam.class);
        verify(mapper, org.mockito.Mockito.times(4)).insert(captor.capture());
        List<ProcessParam> params = captor.getAllValues();
        assertThat(params).extracting(ProcessParam::getStepUuid)
                .containsOnly("step-1");
        assertThat(params).extracting(ProcessParam::getAreaRatio)
                .containsExactly(new BigDecimal("100"), new BigDecimal("100"),
                        new BigDecimal("101"), new BigDecimal("101"));
        assertThat(params).extracting(ProcessParam::getAreaValue)
                .containsExactly(
                        area(30, 3), area(24, 3), area(30, 3), area(24, 3));
    }

    @Test
    void segmentRatioIsNotStoredAsParameterEstimateWeight() {
        ProcessParamMapper mapper = mock(ProcessParamMapper.class);
        ProcessRouteParamWriter writer = new ProcessRouteParamWriter(mapper, new ObjectMapper());
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1);
        RewindSegmentPlanDTO segment = new RewindSegmentPlanDTO();
        segment.setSegmentRatio(new BigDecimal("0.5"));
        RewindLayoutItemPlanDTO item = new RewindLayoutItemPlanDTO();
        item.setWidth(800);
        segment.setLayoutItems(List.of(item));
        stage.getPlan().setSegments(List.of(segment));
        Map<String, ProcessStageOutput> outputs = Map.of("a", output("step-1", 1, "621"));

        writer.write(context(), dto(stage), outputs);

        var captor = org.mockito.ArgumentCaptor.forClass(ProcessParam.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getAreaRatio()).isEqualByComparingTo("621");
    }

    private ProcessRoutePreviewDTO dto(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid("roll-1");
        dto.setStages(List.of(stage));
        return dto;
    }

    private ProcessRoutePreviewDTO.RouteStageDTO stage(int rewindMode) {
        ProcessRoutePreviewDTO.RouteStageDTO stage = new ProcessRoutePreviewDTO.RouteStageDTO();
        stage.setStageLevel(1);
        stage.setStepType(2);
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(2);
        plan.setRewindMode(rewindMode);
        stage.setPlan(plan);
        return stage;
    }

    private ProcessStageOutput output(String stepUuid, int sort, String estimate) {
        ProcessStageOutput output = new ProcessStageOutput();
        output.setUuid("output-" + sort);
        output.setStepUuid(stepUuid);
        output.setStageLevel(1);
        output.setOutputSort(sort);
        output.setEstimateWeight(new BigDecimal(estimate));
        output.setFinishWidth(800);
        return output;
    }

    private com.paper.mes.processorder.dto.FinishConfigSpecDTO.FinishLayerDTO layer(
            int outDiameter, int coreDiameter) {
        var layer = new com.paper.mes.processorder.dto.FinishConfigSpecDTO.FinishLayerDTO();
        layer.setOutDiameter(outDiameter);
        layer.setCoreDiameter(coreDiameter);
        return layer;
    }

    private BigDecimal area(int outDiameter, int coreDiameter) {
        return com.paper.mes.processorder.calc.RewindWeightCalculator.crossSectionArea(
                com.paper.mes.processorder.calc.RewindWeightCalculator.storedDiameterToMm(
                        BigDecimal.valueOf(outDiameter)),
                com.paper.mes.processorder.calc.RewindWeightCalculator.storedCoreDiameterToMm(
                        BigDecimal.valueOf(coreDiameter)));
    }

    private ProcessRouteContext context() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setOrderUuid("order-1");
        roll.setOriginalWidth(2400);
        return new ProcessRouteContext(order, roll);
    }
}
