package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessParamMapper;
import com.paper.mes.processorder.mapper.ProcessStageInputRelMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessRouteCleanupServiceTest {

    @Mock private FinishRollMapper finishRollMapper;
    @Mock private FinishOriginalRelMapper finishOriginalRelMapper;
    @Mock private ProcessStageInputRelMapper stageInputRelMapper;
    @Mock private ProcessStageOutputMapper stageOutputMapper;
    @Mock private ProcessParamMapper processParamMapper;
    @Mock private ProcessStepMapper processStepMapper;

    private ProcessRouteCleanupService service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FinishRoll.class);
    }

    @BeforeEach
    void setUp() {
        service = new ProcessRouteCleanupService(finishRollMapper, finishOriginalRelMapper,
                stageInputRelMapper, stageOutputMapper, processParamMapper, processStepMapper);
    }

    @Test
    void clearExistingRoute_withOldFinishes_voidsThemInOneBatchUpdate() {
        when(finishRollMapper.selectList(any())).thenReturn(List.of(finish("finish-1"), finish("finish-2")));
        when(finishRollMapper.update(isNull(), anyWrapper())).thenReturn(2);

        service.clearExistingRoute(context());

        verify(finishOriginalRelMapper).delete(any());
        verify(finishRollMapper).update(isNull(), anyWrapper());
        verify(finishRollMapper, never()).updateById(any(FinishRoll.class));
        verify(stageInputRelMapper).delete(any());
        verify(stageOutputMapper).delete(any());
        verify(processParamMapper).delete(any());
        verify(processStepMapper).delete(any());
    }

    @Test
    void clearExistingRoute_whenBatchVoidUpdatesFewerRows_raisesConcurrencyError() {
        when(finishRollMapper.selectList(any())).thenReturn(List.of(finish("finish-1"), finish("finish-2")));
        when(finishRollMapper.update(isNull(), anyWrapper())).thenReturn(1);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.clearExistingRoute(context()));

        assertEquals(ErrorCode.E006.getCode(), error.getErrorCode());
        verify(finishRollMapper, never()).updateById(any(FinishRoll.class));
    }

    @Test
    void clearExistingRoute_whenRollIsSourceOfActiveSharedFinish_rejectsPartialCleanup() {
        when(finishOriginalRelMapper.selectList(any())).thenReturn(
                List.of(relation("finish-shared")),
                List.of(relation("finish-shared"), relation("finish-shared", "roll-2")));
        when(finishRollMapper.selectList(any())).thenReturn(List.of(sharedFinish("finish-shared", 1)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.clearExistingRoute(context()));

        assertEquals(ErrorCode.E003.getCode(), error.getErrorCode());
        verify(finishOriginalRelMapper, never()).delete(any());
        verify(finishRollMapper, never()).update(isNull(), anyWrapper());
        verify(processStepMapper, never()).delete(any());
    }

    @Test
    void clearExistingRoute_whenSharedRelationUsesOwnerKey_stillRejectsPartialCleanup() {
        when(finishOriginalRelMapper.selectList(any())).thenReturn(
                List.of(relation("finish-shared")),
                List.of(relation("finish-shared"), relation("finish-shared", "roll-2")));
        when(finishRollMapper.selectList(any())).thenReturn(List.of(finish("finish-shared")));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.clearExistingRoute(context()));

        assertEquals(ErrorCode.E003.getCode(), error.getErrorCode());
        verify(finishOriginalRelMapper, never()).delete(any());
        verify(finishRollMapper, never()).update(isNull(), anyWrapper());
    }

    @Test
    void clearExistingRoute_whenSharedFinishWasAlreadyVoided_removesSourceRelation() {
        when(finishOriginalRelMapper.selectList(any())).thenReturn(List.of(relation("finish-shared")));
        when(finishRollMapper.selectList(any())).thenReturn(List.of(sharedFinish("finish-shared", 3)));

        service.clearExistingRoute(context());

        verify(finishOriginalRelMapper, times(1)).delete(any());
        verify(finishRollMapper, never()).update(isNull(), anyWrapper());
        verify(processStepMapper).delete(any());
    }

    private ProcessRouteContext context() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setRollNo("R001");
        return new ProcessRouteContext(order, roll);
    }

    private FinishRoll finish(String uuid) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setRollNoStatus(1);
        finish.setOriginalRollNos("R001");
        return finish;
    }

    private FinishRoll sharedFinish(String uuid, int rollNoStatus) {
        FinishRoll finish = finish(uuid);
        finish.setOriginalRollNos("OTHER-ROLL");
        finish.setRollNoStatus(rollNoStatus);
        return finish;
    }

    private FinishOriginalRel relation(String finishUuid) {
        return relation(finishUuid, "roll-1");
    }

    private FinishOriginalRel relation(String finishUuid, String originalUuid) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setFinishUuid(finishUuid);
        relation.setOriginalUuid(originalUuid);
        return relation;
    }

    private Wrapper<FinishRoll> anyWrapper() {
        return any();
    }
}
