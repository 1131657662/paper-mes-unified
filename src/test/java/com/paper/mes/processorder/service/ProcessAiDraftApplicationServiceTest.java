package com.paper.mes.processorder.service;

import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.compile.ProcessAiPackagingCandidate;
import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import com.paper.mes.ai.process.compile.ProcessAiRollConfiguration;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.dto.DraftRollProcessDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiDraftApplicationServiceTest {

    @BeforeAll
    static void initializeProcessOrderMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ProcessOrder.class);
    }

    @Test
    void applyWithoutAcceptedMachinePreservesTheExistingManualMachine() {
        ProcessOrderMapper orderMapper = mock(ProcessOrderMapper.class);
        OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
        ProcessAiDraftPlanApplier planApplier = mock(ProcessAiDraftPlanApplier.class);
        DraftRollProcessManager rollManager = mock(DraftRollProcessManager.class);
        ProcessOrderService orderService = mock(ProcessOrderService.class);
        ProcessAiDraftApplicationService service = new ProcessAiDraftApplicationService(
                mock(BusinessLockService.class), mock(DraftOrderVersionGuard.class), orderMapper,
                planApplier, rollManager, rollMapper, mock(ServiceStepBatchUpsertWriter.class),
                mock(ProcessAiServiceStepRequestFactory.class), orderService);
        ProcessOrder order = order();
        OriginalRoll roll = roll();
        when(orderMapper.selectById("order-1")).thenReturn(order);
        when(orderMapper.update(any(), any())).thenReturn(1);
        when(rollMapper.selectBatchIds(List.of("roll-1"))).thenReturn(List.of(roll));
        when(planApplier.apply(any(), any(), any())).thenReturn(Map.of());

        service.apply(new ProcessAiDraftApplyCommand("order-1", 7, "parse-1", "{}", null,
                List.of("/assignments/R1/sourceRollRefs", "/assignments/R1/processMode"),
                new ProcessAiCompilationResult(true,
                        List.of(new ProcessAiRollConfiguration("R1", List.of("roll-1"), 1, 2)),
                        List.of(), List.of(), List.of(), List.of())));

        ArgumentCaptor<List<DraftRollProcessDTO>> applied = ArgumentCaptor.forClass(List.class);
        verify(rollManager).applyLocked(eq(order), applied.capture());
        assertThat(applied.getValue()).singleElement()
                .extracting(DraftRollProcessDTO::getMachineUuid)
                .isEqualTo("manual-machine");
    }

    @Test
    void applyPackagingOnlyDoesNotOverwriteStepThreeConfiguration() {
        ProcessOrderMapper orderMapper = mock(ProcessOrderMapper.class);
        OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
        ProcessAiDraftPlanApplier planApplier = mock(ProcessAiDraftPlanApplier.class);
        DraftRollProcessManager rollManager = mock(DraftRollProcessManager.class);
        ServiceStepBatchUpsertWriter stepWriter = mock(ServiceStepBatchUpsertWriter.class);
        ProcessAiServiceStepRequestFactory requestFactory = mock(ProcessAiServiceStepRequestFactory.class);
        ProcessOrderService orderService = mock(ProcessOrderService.class);
        ProcessAiDraftApplicationService service = new ProcessAiDraftApplicationService(
                mock(BusinessLockService.class), mock(DraftOrderVersionGuard.class), orderMapper,
                planApplier, rollManager, rollMapper, stepWriter, requestFactory, orderService);
        ProcessOrder order = order();
        OriginalRoll roll = roll();
        roll.setProcessMode(ProcessModePolicy.STANDARD);
        ProcessAiPackagingCandidate packaging = new ProcessAiPackagingCandidate(
                "R1", "roll-1", List.of(), 3, "STRIP_SORT", "剥损整理", "PIECE",
                null, 1, java.math.BigDecimal.valueOf(20), null, "客户要求");
        ProcessStepDTO step = new ProcessStepDTO();
        when(orderMapper.selectById("order-1")).thenReturn(order);
        when(orderMapper.update(any(), any())).thenReturn(1);
        when(rollMapper.selectBatchIds(List.of("roll-1"))).thenReturn(List.of(roll));
        when(planApplier.apply(any(), any(), any())).thenReturn(Map.of());
        when(requestFactory.create(packaging, roll)).thenReturn(step);

        service.apply(new ProcessAiDraftApplyCommand("order-1", 7, "parse-1", "{}", null,
                List.of("/assignments/R1/ancillaryRequirements/packaging"),
                new ProcessAiCompilationResult(true,
                        List.of(new ProcessAiRollConfiguration("R1", List.of("roll-1"),
                                ProcessModePolicy.SERVICE_ONLY, null)),
                        List.of(), List.of(packaging), List.of(), List.of())));

        ArgumentCaptor<List<DraftRollProcessDTO>> applied = ArgumentCaptor.forClass(List.class);
        verify(rollManager).applyLocked(eq(order), applied.capture());
        assertThat(applied.getValue()).isEmpty();
        verify(stepWriter).upsert(eq("order-1"), eq(List.of(step)), any());
        verify(orderService).calcFee("order-1");
    }

    @Test
    void applyMachineOnlyPreservesTheExistingModeAndMainProcess() {
        ProcessOrderMapper orderMapper = mock(ProcessOrderMapper.class);
        OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
        ProcessAiDraftPlanApplier planApplier = mock(ProcessAiDraftPlanApplier.class);
        DraftRollProcessManager rollManager = mock(DraftRollProcessManager.class);
        ProcessOrderService orderService = mock(ProcessOrderService.class);
        ProcessAiDraftApplicationService service = new ProcessAiDraftApplicationService(
                mock(BusinessLockService.class), mock(DraftOrderVersionGuard.class), orderMapper,
                planApplier, rollManager, rollMapper, mock(ServiceStepBatchUpsertWriter.class),
                mock(ProcessAiServiceStepRequestFactory.class), orderService);
        ProcessOrder order = order();
        OriginalRoll roll = roll();
        roll.setProcessMode(ProcessModePolicy.STANDARD);
        roll.setMainStepType(2);
        ProcessPlanDTO candidatePlan = new ProcessPlanDTO();
        candidatePlan.setMachineUuid("ai-machine");
        when(orderMapper.selectById("order-1")).thenReturn(order);
        when(orderMapper.update(any(), any())).thenReturn(1);
        when(rollMapper.selectBatchIds(List.of("roll-1"))).thenReturn(List.of(roll));
        when(planApplier.apply(any(), any(), any())).thenReturn(Map.of());

        service.apply(new ProcessAiDraftApplyCommand("order-1", 7, "parse-1", "{}", null,
                List.of("/assignments/R1/machineUuid"),
                new ProcessAiCompilationResult(true,
                        List.of(new ProcessAiRollConfiguration("R1", List.of("roll-1"),
                                ProcessModePolicy.SERVICE_ONLY, 1)),
                        List.of(new ProcessAiCompiledPlan("R1", "roll-1", List.of(),
                                candidatePlan, new com.paper.mes.processorder.dto.PlanPreviewVO())),
                        List.of(), List.of(), List.of())));

        ArgumentCaptor<List<DraftRollProcessDTO>> applied = ArgumentCaptor.forClass(List.class);
        verify(rollManager).applyLocked(eq(order), applied.capture());
        assertThat(applied.getValue()).singleElement().satisfies(item -> {
            assertThat(item.getProcessMode()).isEqualTo(ProcessModePolicy.STANDARD);
            assertThat(item.getMainStepType()).isEqualTo(2);
            assertThat(item.getMachineUuid()).isEqualTo("ai-machine");
        });
    }

    private ProcessOrder order() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(0);
        order.setVersion(7);
        return order;
    }

    private OriginalRoll roll() {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setOrderUuid("order-1");
        roll.setMachineUuid("manual-machine");
        return roll;
    }
}
