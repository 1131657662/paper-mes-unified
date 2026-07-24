package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessConcurrencyGuardTest {

    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), FinishRoll.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), FinishOriginalRel.class);
    }

    @Test
    void saveProcessPlan_whenRollUpdateConflicts_raisesConcurrencyError() {
        ProcessOrderMapper orderMapper = mock(ProcessOrderMapper.class);
        OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
        DraftOrderVersionGuard versionGuard = mock(DraftOrderVersionGuard.class);
        ProcessOrder order = order();
        order.setOrderStatus(0);
        order.setVersion(1);
        when(orderMapper.selectById("order-1")).thenReturn(order);
        when(rollMapper.selectById("roll-1")).thenReturn(roll());
        when(rollMapper.updateById(any(OriginalRoll.class))).thenReturn(0);
        ProcessPlanDraftManager manager = new ProcessPlanDraftManager(
                orderMapper, rollMapper, mock(ProcessConfigDraftMapper.class), mock(ProcessOrderService.class),
                mock(ProcessPlanMapper.class), mock(SawPlanPreviewer.class), mock(OnSitePlanPreviewer.class),
                new ObjectMapper(), mock(BusinessLockService.class), mock(ServiceOnlyProcessPolicy.class), versionGuard);
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(3);

        assertConcurrency(() -> manager.saveProcessPlan("order-1", "roll-1", plan, 1));
        verify(versionGuard).advance("order-1", 1);
    }

    @Test
    void replaceRoute_whenRollUpdateConflicts_stopsBeforeWritingSteps() {
        OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
        ProcessRouteStepWriter stepWriter = mock(ProcessRouteStepWriter.class);
        ProcessRouteFinishWriter finishWriter = mock(ProcessRouteFinishWriter.class);
        when(rollMapper.updateById(any(OriginalRoll.class))).thenReturn(0);
        ProcessRoutePersistenceService service = new ProcessRoutePersistenceService(
                rollMapper, mock(ProcessRouteCleanupService.class), stepWriter, finishWriter);
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        ProcessRoutePreviewDTO.RouteStageDTO stage = new ProcessRoutePreviewDTO.RouteStageDTO();
        stage.setStepType(1);
        dto.setStages(List.of(stage));

        assertConcurrency(() -> service.replaceRoute(context(), dto, new ProcessRoutePreviewVO()));
        verify(stepWriter, never()).write(any(), any(), any());
        verify(finishWriter, never()).createFinalFinishes(any(), any(), any());
    }

    @Test
    void createFinalFinishes_whenOutputUpdateConflicts_raisesConcurrencyError() {
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        ProcessStageOutputMapper outputMapper = mock(ProcessStageOutputMapper.class);
        RollNoSequenceService sequence = mock(RollNoSequenceService.class);
        when(sequence.nextFinishRollNo()).thenReturn("C001");
        when(finishMapper.insert(any(FinishRoll.class))).thenAnswer(invocation -> {
            invocation.<FinishRoll>getArgument(0).setUuid("finish-1");
            return 1;
        });
        when(outputMapper.updateById(any(ProcessStageOutput.class))).thenReturn(0);
        ProcessRouteFinishWriter writer = new ProcessRouteFinishWriter(
                finishMapper, mock(FinishOriginalRelMapper.class), outputMapper, sequence);
        ProcessRoutePreviewVO.RouteOutputVO row = new ProcessRoutePreviewVO.RouteOutputVO();
        row.setOutputKey("output-1");
        row.setConsumedByNextStage(false);
        row.setEstimateWeight(BigDecimal.ONE);
        ProcessRoutePreviewVO preview = new ProcessRoutePreviewVO();
        preview.setOutputs(List.of(row));
        ProcessStageOutput output = new ProcessStageOutput();
        output.setUuid("output-uuid-1");

        assertConcurrency(() -> writer.createFinalFinishes(context(), preview, Map.of("output-1", output)));
    }

    @Test
    void consume_whenOutputUpdateConflicts_raisesConcurrencyError() {
        ProcessStageOutputMapper outputMapper = mock(ProcessStageOutputMapper.class);
        when(outputMapper.updateById(any(ProcessStageOutput.class))).thenReturn(0);
        ProcessRouteSourceConsumer consumer = new ProcessRouteSourceConsumer(
                outputMapper, mock(FinishRollMapper.class), mock(FinishOriginalRelMapper.class));
        ProcessStageOutput output = new ProcessStageOutput();
        output.setUuid("output-1");

        assertConcurrency(() -> consumer.consume(List.of(output)));
    }

    @Test
    void consume_whenLinkedFinishUpdateConflicts_stopsBeforeOutputUpdate() {
        ProcessStageOutputMapper outputMapper = mock(ProcessStageOutputMapper.class);
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        when(finishMapper.selectById("finish-1")).thenReturn(finish);
        when(finishMapper.updateById(finish)).thenReturn(0);
        ProcessRouteSourceConsumer consumer = new ProcessRouteSourceConsumer(
                outputMapper, finishMapper, mock(FinishOriginalRelMapper.class));
        ProcessStageOutput output = new ProcessStageOutput();
        output.setUuid("output-1");
        output.setFinishRollUuid("finish-1");

        assertConcurrency(() -> consumer.consume(List.of(output)));
        verify(outputMapper, never()).updateById(any(ProcessStageOutput.class));
    }

    private void assertConcurrency(Runnable action) {
        BusinessException error = assertThrows(BusinessException.class, action::run);
        assertEquals(ErrorCode.E006.getCode(), error.getErrorCode());
    }

    private ProcessRouteContext context() {
        return new ProcessRouteContext(order(), roll());
    }

    private ProcessOrder order() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setWarehouseUuid("warehouse-1");
        return order;
    }

    private OriginalRoll roll() {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setOrderUuid("order-1");
        roll.setRollNo("R001");
        return roll;
    }
}
