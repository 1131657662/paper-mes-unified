package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessRouteExistingOutputResolverTest {

    @Mock private ProcessStageOutputMapper stageOutputMapper;
    @Mock private FinishRollMapper finishRollMapper;
    @Mock private FinishOriginalRelMapper finishOriginalRelMapper;
    @Mock private ProcessStepMapper processStepMapper;

    private ProcessRouteExistingOutputResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ProcessRouteExistingOutputResolver(
                stageOutputMapper, finishRollMapper, finishOriginalRelMapper, processStepMapper);
    }

    @Test
    void resolveForPreview_whenSourceFinishIsScrapped_rejectsLaterProcessing() {
        ProcessStageOutput output = new ProcessStageOutput();
        output.setUuid("output-1");
        output.setFinishRollUuid("finish-1");
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setFinishStatus(4);
        when(stageOutputMapper.selectList(any())).thenReturn(List.of(output));
        when(finishRollMapper.selectById("finish-1")).thenReturn(finish);

        assertThatThrownBy(() -> resolver.resolveForPreview(context(), route("output-1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已报废成品不能作为后续工艺来源");
    }

    private ProcessRouteContext context() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        return new ProcessRouteContext(order, roll);
    }

    private ProcessRoutePreviewDTO route(String inputKey) {
        ProcessRoutePreviewDTO.RouteStageDTO stage = new ProcessRoutePreviewDTO.RouteStageDTO();
        stage.setInputOutputKeys(List.of(inputKey));
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setStages(List.of(stage));
        return dto;
    }
}
