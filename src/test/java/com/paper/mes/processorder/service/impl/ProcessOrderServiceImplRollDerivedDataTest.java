package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.dto.FeeResultVO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
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
import com.paper.mes.processorder.service.ProcessRouteCleanupService;
import com.paper.mes.processorder.service.RollNoSequenceService;
import com.paper.mes.processorder.service.SawPlanPreviewer;
import com.paper.mes.processorder.service.WeightCheckThresholdService;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.system.config.service.DocumentNoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessOrderServiceImplRollDerivedDataTest {

    private final OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
    private final ProcessRouteCleanupService cleanupService = mock(ProcessRouteCleanupService.class);
    private final ProcessOrder order = order();
    private final OriginalRoll roll = roll();
    private TrackingService service;

    @BeforeEach
    void setUp() {
        when(rollMapper.selectById("roll-1")).thenReturn(roll);
        when(rollMapper.updateById(any(OriginalRoll.class))).thenReturn(1);
        when(rollMapper.deleteById("roll-1")).thenReturn(1);
        service = service();
    }

    @Test
    void updateRoll_whenStructureChanges_recalculatesDerivedData() {
        OriginalRollDTO dto = rollDto(new BigDecimal("1200.000"));

        service.updateRoll("roll-1", dto);

        assertEquals("order-1", service.recalculatedOrderUuid);
        verify(cleanupService).clearExistingRoute(any());
    }

    @Test
    void deleteRoll_afterCleanup_recalculatesDerivedData() {
        service.deleteRoll("roll-1");

        assertEquals("order-1", service.recalculatedOrderUuid);
        verify(rollMapper).deleteById("roll-1");
    }

    @Test
    void deleteRoll_whenDeleteConflicts_throwsAndSkipsRecalculation() {
        when(rollMapper.deleteById("roll-1")).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class, () -> service.deleteRoll("roll-1"));

        assertEquals(ErrorCode.E006.getCode(), error.getErrorCode());
        assertNull(service.recalculatedOrderUuid);
    }

    @Test
    void addRoll_afterInsert_recalculatesDerivedData() {
        service.addRoll("order-1", rollDto(new BigDecimal("800.000")));

        assertEquals("order-1", service.recalculatedOrderUuid);
    }

    private TrackingService service() {
        return new TrackingService(order, rollMapper, cleanupService);
    }

    private ProcessOrder order() {
        ProcessOrder value = new ProcessOrder();
        value.setUuid("order-1");
        value.setOrderStatus(1);
        return value;
    }

    private OriginalRoll roll() {
        OriginalRoll value = new OriginalRoll();
        value.setUuid("roll-1");
        value.setOrderUuid("order-1");
        value.setRollNo("R001");
        value.setPaperName("测试纸");
        value.setGramWeight(100);
        value.setOriginalWidth(1000);
        value.setRollWeight(new BigDecimal("1000.000"));
        value.setPieceNum(1);
        value.setProcessMode(3);
        value.setRollStatus(1);
        value.setVersion(1);
        return value;
    }

    private OriginalRollDTO rollDto(BigDecimal weight) {
        OriginalRollDTO dto = new OriginalRollDTO();
        dto.setRollNo("R001");
        dto.setPaperName("测试纸");
        dto.setGramWeight(100);
        dto.setOriginalWidth(1000);
        dto.setRollWeight(weight);
        dto.setPieceNum(1);
        dto.setProcessMode(3);
        return dto;
    }

    private static final class TrackingService extends ProcessOrderServiceImpl {
        private final ProcessOrder order;
        private String recalculatedOrderUuid;

        TrackingService(ProcessOrder order, OriginalRollMapper rollMapper,
                        ProcessRouteCleanupService cleanupService) {
            super(rollMapper, mock(FinishRollMapper.class), mock(ProcessStepMapper.class),
                    mock(ProcessParamMapper.class), mock(ProcessStageInputRelMapper.class),
                    mock(ProcessStageOutputMapper.class), mock(FinishOriginalRelMapper.class),
                    mock(DeliveryDetailMapper.class), mock(SettleDetailMapper.class),
                    mock(CustomerService.class), mock(OperationLogService.class), new ObjectMapper(),
                    mock(DamageImageService.class), mock(RollNoSequenceService.class), new SawPlanPreviewer(),
                    mock(DocumentNoService.class), mock(BusinessLockService.class), mock(MachineMapper.class),
                    mock(WeightCheckThresholdService.class), null, null, null, new BackRecordScopeResolver(),
                    null, mock(BackRecordWarehousePolicy.class), null, null, null, null, null, null, null,
                    cleanupService);
            this.order = order;
        }

        @Override
        public ProcessOrder getById(java.io.Serializable id) {
            return order;
        }

        @Override
        public FeeResultVO calcFee(String uuid) {
            recalculatedOrderUuid = uuid;
            return new FeeResultVO();
        }

        @Override
        public boolean updateById(ProcessOrder entity) {
            return true;
        }
    }
}
