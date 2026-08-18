package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.ai.process.parse.ProcessAiPackagingCandidateResolutionService;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.dto.ProcessStepBatchDTO;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DraftServiceStepServiceTest {

    private ProcessOrderMapper orderMapper;
    private ProcessStepMapper stepMapper;
    private ProcessOrderService orderService;
    private DraftOrderVersionGuard versionGuard;
    private ProcessAiPackagingCandidateResolutionService packagingResolution;
    private DraftServiceStepService service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(ProcessOrderMapper.class);
        stepMapper = mock(ProcessStepMapper.class);
        orderService = mock(ProcessOrderService.class);
        versionGuard = mock(DraftOrderVersionGuard.class);
        packagingResolution = mock(ProcessAiPackagingCandidateResolutionService.class);
        service = new DraftServiceStepService(
                mock(BusinessLockService.class), orderMapper, stepMapper, orderService,
                versionGuard, packagingResolution);
    }

    @Test
    void add_allowsAdditionalProcessOnDraftOrder() {
        when(orderMapper.selectById("order-1")).thenReturn(order(0));
        ProcessStepDTO request = request(3, 0);

        service.add("order-1", request);

        verify(orderService).addProcessStep("order-1", request);
        verify(packagingResolution).markSaved("order-1", List.of(request));
        verify(versionGuard).assertExpected(org.mockito.ArgumentMatchers.any(ProcessOrder.class),
                org.mockito.ArgumentMatchers.eq(5));
    }

    @Test
    void add_withoutExpectedVersion_rejectsBeforeReadingOrWriting() {
        ProcessStepDTO request = request(3, 0);
        request.setExpectedVersion(null);

        assertThatThrownBy(() -> service.add("order-1", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("版本缺失");

        verifyNoInteractions(orderMapper, stepMapper, orderService, versionGuard);
    }

    @Test
    void addBatch_rejectsMainOrProductionProcesses() {
        when(orderMapper.selectById("order-1")).thenReturn(order(0));
        ProcessStepBatchDTO batch = new ProcessStepBatchDTO();
        batch.setSteps(List.of(request(2, 1)));

        assertThatThrownBy(() -> service.addBatch("order-1", batch))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(orderService);
    }

    @Test
    void update_rejectsAdditionalProcessAfterDraftStage() {
        ProcessStep step = step(3, 0);
        when(stepMapper.selectById("step-1")).thenReturn(step);
        when(orderMapper.selectById("order-1")).thenReturn(order(1));

        assertThatThrownBy(() -> service.update("step-1", request(3, 0)))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(orderService);
    }

    @Test
    void delete_rejectsMainProcessEvenOnDraftOrder() {
        ProcessStep step = step(3, 1);
        when(stepMapper.selectById("step-1")).thenReturn(step);
        when(orderMapper.selectById("order-1")).thenReturn(order(0));

        assertThatThrownBy(() -> service.delete("step-1", 5))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(orderService);
    }

    private ProcessOrder order(int status) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(status);
        order.setVersion(5);
        return order;
    }

    private ProcessStepDTO request(int stepType, int isMain) {
        ProcessStepDTO request = new ProcessStepDTO();
        request.setOriginalUuid("roll-1");
        request.setStepType(stepType);
        request.setIsMain(isMain);
        request.setExpectedVersion(5);
        return request;
    }

    private ProcessStep step(int stepType, int isMain) {
        ProcessStep step = new ProcessStep();
        step.setUuid("step-1");
        step.setOrderUuid("order-1");
        step.setOriginalUuid("roll-1");
        step.setStepType(stepType);
        step.setIsMain(isMain);
        return step;
    }
}
