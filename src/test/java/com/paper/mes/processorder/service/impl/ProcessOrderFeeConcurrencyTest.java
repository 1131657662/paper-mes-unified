package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessParamMapper;
import com.paper.mes.processorder.mapper.ProcessStageInputRelMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.processorder.service.BackRecordScopeResolver;
import com.paper.mes.processorder.service.BackRecordWarehousePolicy;
import com.paper.mes.processorder.service.DamageImageService;
import com.paper.mes.processorder.service.RollNoSequenceService;
import com.paper.mes.processorder.service.SawPlanPreviewer;
import com.paper.mes.processorder.service.WeightCheckThresholdService;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.system.config.service.DocumentNoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessOrderFeeConcurrencyTest {

    private final OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
    private final ProcessStepMapper stepMapper = mock(ProcessStepMapper.class);
    private final FinishRollMapper finishMapper = mock(FinishRollMapper.class);
    private FeeService service;

    @BeforeEach
    void setUp() {
        when(rollMapper.selectList(any())).thenReturn(List.of(roll()));
        when(stepMapper.selectList(any())).thenReturn(List.of(step()));
        when(finishMapper.selectList(any())).thenReturn(List.of());
        service = new FeeService(order(), rollMapper, stepMapper, finishMapper);
    }

    @Test
    void calcFee_whenStepUpdateConflicts_raisesConcurrencyError() {
        when(stepMapper.updateById(any(ProcessStep.class))).thenReturn(0);

        assertConcurrency(() -> service.calcFee("order-1"));
    }

    @Test
    void calcFee_whenRollUpdateConflicts_raisesConcurrencyError() {
        when(stepMapper.updateById(any(ProcessStep.class))).thenReturn(1);
        when(rollMapper.updateById(any(OriginalRoll.class))).thenReturn(0);

        assertConcurrency(() -> service.calcFee("order-1"));
    }

    @Test
    void calcFee_whenOrderUpdateConflicts_raisesConcurrencyError() {
        when(stepMapper.updateById(any(ProcessStep.class))).thenReturn(1);
        when(rollMapper.updateById(any(OriginalRoll.class))).thenReturn(1);
        service.orderUpdated = false;

        assertConcurrency(() -> service.calcFee("order-1"));
    }

    private void assertConcurrency(Runnable action) {
        BusinessException error = assertThrows(BusinessException.class, action::run);
        assertEquals(ErrorCode.E006.getCode(), error.getErrorCode());
    }

    private ProcessOrder order() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(1);
        order.setTaxRate(BigDecimal.ZERO);
        return order;
    }

    private OriginalRoll roll() {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setActualWeight(new BigDecimal("1000"));
        return roll;
    }

    private ProcessStep step() {
        ProcessStep step = new ProcessStep();
        step.setUuid("step-1");
        step.setOriginalUuid("roll-1");
        step.setStepType(1);
        step.setKnifeCount(1);
        step.setUnitPrice(BigDecimal.ONE);
        step.setIsMain(1);
        return step;
    }

    private static final class FeeService extends ProcessOrderServiceImpl {
        private final ProcessOrder order;
        private boolean orderUpdated = true;

        FeeService(ProcessOrder order, OriginalRollMapper rollMapper, ProcessStepMapper stepMapper,
                   FinishRollMapper finishMapper) {
            super(rollMapper, finishMapper, stepMapper, mock(ProcessParamMapper.class),
                    mock(ProcessStageInputRelMapper.class), mock(ProcessStageOutputMapper.class),
                    mock(FinishOriginalRelMapper.class), mock(DeliveryDetailMapper.class),
                    mock(SettleDetailMapper.class), mock(CustomerService.class),
                    mock(OperationLogService.class), new ObjectMapper(), mock(DamageImageService.class),
                    mock(RollNoSequenceService.class), new SawPlanPreviewer(), mock(DocumentNoService.class),
                    mock(BusinessLockService.class), mock(MachineMapper.class), mock(WeightCheckThresholdService.class),
                    null, null, null, new BackRecordScopeResolver(), null, mock(BackRecordWarehousePolicy.class),
                    null, null, null, null, null, null, null, null);
            this.order = order;
        }

        @Override
        public ProcessOrder getById(java.io.Serializable id) {
            return order;
        }

        @Override
        public boolean updateById(ProcessOrder entity) {
            return orderUpdated;
        }
    }
}
