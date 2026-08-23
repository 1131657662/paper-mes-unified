package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStageInputRel;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessStageInputRelMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessRouteFinishWriterSourceTraceTest {

    @Test
    void createFinalFinishes_tracesTwoUpstreamSourcesByInputWeight() {
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        FinishOriginalRelMapper relationMapper = mock(FinishOriginalRelMapper.class);
        ProcessStageOutputMapper outputMapper = mock(ProcessStageOutputMapper.class);
        ProcessStageInputRelMapper inputMapper = mock(ProcessStageInputRelMapper.class);
        RollNoSequenceService sequence = mock(RollNoSequenceService.class);
        when(sequence.nextFinishRollNo()).thenReturn("F001");
        when(finishMapper.insert(any(FinishRoll.class))).thenAnswer(invocation -> {
            invocation.<FinishRoll>getArgument(0).setUuid("finish-final");
            return 1;
        });
        when(outputMapper.updateById(any(ProcessStageOutput.class))).thenReturn(1);

        ProcessStageOutput left = output("left", "f-left", "60");
        ProcessStageOutput right = output("right", "f-right", "40");
        ProcessStageOutput finalOutput = output("final", null, "100");
        finalOutput.setStepUuid("step-final");
        ProcessStageInputRel leftInput = input("left", "roll-left");
        ProcessStageInputRel rightInput = input("right", "roll-right");
        leftInput.setStepUuid("step-final");
        rightInput.setStepUuid("step-final");
        when(inputMapper.selectList(any())).thenReturn(List.of(leftInput, rightInput));
        when(relationMapper.selectList(any())).thenReturn(List.of(
                relation("f-left", "roll-left"), relation("f-right", "roll-right")));

        ProcessRouteFinishWriter writer = new ProcessRouteFinishWriter(
                finishMapper, relationMapper, outputMapper, sequence, inputMapper);
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-main");
        ProcessRoutePreviewVO.RouteOutputVO line = new ProcessRoutePreviewVO.RouteOutputVO();
        line.setOutputKey("final");
        line.setEstimateWeight(new BigDecimal("100"));
        ProcessRoutePreviewVO preview = new ProcessRoutePreviewVO();
        preview.setOutputs(List.of(line));

        writer.createFinalFinishes(new ProcessRouteContext(order, roll), preview,
                Map.of("left", left, "right", right, "final", finalOutput));

        var inserted = org.mockito.ArgumentCaptor.forClass(FinishOriginalRel.class);
        org.mockito.Mockito.verify(relationMapper, org.mockito.Mockito.times(2)).insert(inserted.capture());
        assertThat(inserted.getAllValues()).extracting(FinishOriginalRel::getOriginalUuid)
                .containsExactlyInAnyOrder("roll-left", "roll-right");
        assertThat(inserted.getAllValues()).extracting(FinishOriginalRel::getShareRatio)
                .containsExactlyInAnyOrder(new BigDecimal("60.00"), new BigDecimal("40.00"));
    }

    private ProcessStageOutput output(String uuid, String finishUuid, String weight) {
        ProcessStageOutput output = new ProcessStageOutput();
        output.setUuid(uuid);
        output.setFinishRollUuid(finishUuid);
        output.setEstimateWeight(new BigDecimal(weight));
        output.setOriginalUuid("roll-main");
        return output;
    }

    private ProcessStageInputRel input(String outputUuid, String originalUuid) {
        ProcessStageInputRel input = new ProcessStageInputRel();
        input.setInputOutputUuid(outputUuid);
        input.setOriginalUuid(originalUuid);
        return input;
    }

    private FinishOriginalRel relation(String finishUuid, String originalUuid) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setFinishUuid(finishUuid);
        relation.setOriginalUuid(originalUuid);
        relation.setShareRatio(new BigDecimal("100.00"));
        return relation;
    }
}
