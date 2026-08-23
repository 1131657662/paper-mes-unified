package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.entity.ProcessStep;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        ProcessRouteExistingOutputLoader loader = new ProcessRouteExistingOutputLoader(
                stageOutputMapper, finishRollMapper, finishOriginalRelMapper, processStepMapper);
        resolver = new ProcessRouteExistingOutputResolver(stageOutputMapper, loader);
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
        when(finishRollMapper.selectList(any())).thenReturn(List.of(finish));

        assertThatThrownBy(() -> resolver.resolveForPreview(context(), route("output-1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已报废成品不能作为后续工艺来源");
    }

    @Test
    void resolveForPreview_withMultipleExistingOutputs_loadsSourceCollectionsOnce() {
        when(stageOutputMapper.selectList(any())).thenReturn(List.of(
                output("output-1", "finish-1"), output("output-2", "finish-2")));
        when(finishRollMapper.selectList(any())).thenReturn(List.of(
                finish("finish-1", "F001"), finish("finish-2", "F002")));

        Map<String, ProcessStageOutput> result = resolver.resolveForPreview(
                context(), route("output-1", "output-2"));

        assertThat(result).containsOnlyKeys("output-1", "output-2");
        verify(stageOutputMapper, times(1)).selectList(any());
        verify(finishRollMapper, times(1)).selectList(any());
        verify(finishRollMapper, never()).selectById(any());
        verifyNoInteractions(finishOriginalRelMapper, processStepMapper);
    }

    @Test
    void relatedFinishUuids_withNoFinishes_skipsEmptyInQuery() {
        ProcessRouteExistingOutputLoader loader = new ProcessRouteExistingOutputLoader(
                stageOutputMapper, finishRollMapper, finishOriginalRelMapper, processStepMapper);

        assertThat(loader.relatedFinishUuids(context(), List.of())).isEmpty();

        verifyNoInteractions(finishOriginalRelMapper);
    }

    @Test
    void resolveForPreview_withMultipleLegacyFinishes_batchesFallbackQueries() {
        when(stageOutputMapper.selectList(any())).thenReturn(List.of());
        when(finishRollMapper.selectList(any())).thenReturn(List.of(
                finish("finish-1", "F001"), finish("finish-2", "F002")));
        when(finishOriginalRelMapper.selectList(any())).thenReturn(List.of(
                relation("finish-1"), relation("finish-2")));
        when(processStepMapper.selectOne(any())).thenReturn(step());

        Map<String, ProcessStageOutput> result = resolver.resolveForPreview(
                context(), route("F001", "F002"));

        assertThat(result.get("F001").getFinishRollUuid()).isEqualTo("finish-1");
        assertThat(result.get("F002").getFinishRollUuid()).isEqualTo("finish-2");
        verify(finishOriginalRelMapper, times(1)).selectList(any());
        verify(processStepMapper, times(1)).selectOne(any());
        verify(stageOutputMapper, never()).insert(any(ProcessStageOutput.class));
    }

    @Test
    void resolveForSave_whenLegacyFinishIsUnrelated_rejectsBeforeStepReadOrInsert() {
        when(stageOutputMapper.selectList(any())).thenReturn(List.of());
        when(finishRollMapper.selectList(any())).thenReturn(List.of(
                finish("finish-1", "F001"), finish("finish-2", "F002")));
        when(finishOriginalRelMapper.selectList(any())).thenReturn(List.of(relation("finish-1")));

        assertThatThrownBy(() -> resolver.resolveForSave(context(), route("F001", "F002")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("成品卷不属于当前来源母卷");
        verify(finishOriginalRelMapper, times(1)).selectList(any());
        verifyNoInteractions(processStepMapper);
        verify(stageOutputMapper, never()).insert(any(ProcessStageOutput.class));
    }

    private ProcessRouteContext context() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        return new ProcessRouteContext(order, roll);
    }

    private ProcessRoutePreviewDTO route(String... inputKeys) {
        ProcessRoutePreviewDTO.RouteStageDTO stage = new ProcessRoutePreviewDTO.RouteStageDTO();
        stage.setInputOutputKeys(List.of(inputKeys));
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setStages(List.of(stage));
        return dto;
    }

    private ProcessStageOutput output(String uuid, String finishUuid) {
        ProcessStageOutput output = new ProcessStageOutput();
        output.setUuid(uuid);
        output.setFinishRollUuid(finishUuid);
        return output;
    }

    private FinishRoll finish(String uuid, String rollNo) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setFinishRollNo(rollNo);
        return finish;
    }

    private FinishOriginalRel relation(String finishUuid) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setFinishUuid(finishUuid);
        relation.setOriginalUuid("roll-1");
        return relation;
    }

    private ProcessStep step() {
        ProcessStep step = new ProcessStep();
        step.setUuid("step-1");
        step.setStageLevel(1);
        return step;
    }
}
