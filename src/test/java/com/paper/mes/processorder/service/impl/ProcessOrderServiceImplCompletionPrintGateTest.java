package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.inventory.service.InventoryLedgerBusinessRecorder;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.dto.PrintDTO;
import com.paper.mes.processorder.dto.PrintResultVO;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ProcessOrderServiceImplCompletionPrintGateTest {

    private ProcessOrder order;
    private TrackingService service;

    @BeforeEach
    void setUp() {
        order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(2);
        service = new TrackingService(order);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void completeProcessing_withoutConfirmedPrint_rejectsBeforeStatusWrite(Integer printCount) {
        order.setPrintCount(printCount);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.completeProcessing(order.getUuid(), "车间完成"));

        assertEquals(ErrorCode.E003.getCode(), error.getErrorCode());
        assertTrue(error.getMessage().contains("人工确认一次打印"));
        assertTrue(error.getMessage().contains("不代表打印机设备回执"));
        assertEquals(2, order.getOrderStatus());
        assertFalse(service.updated);
    }

    @Test
    void completeProcessing_withConfirmedPrint_movesToRecord() {
        order.setPrintCount(1);
        order.setPrintStatus(1);

        service.completeProcessing(order.getUuid(), "车间完成");

        assertEquals(3, order.getOrderStatus());
        assertTrue(service.updated);
    }

    @Test
    void completeProcessing_withCountOnlyRejectsUnconfirmedRecord() {
        order.setPrintCount(1);
        order.setPrintStatus(0);

        assertThrows(BusinessException.class,
                () -> service.completeProcessing(order.getUuid(), "车间完成"));
        assertEquals(2, order.getOrderStatus());
        assertFalse(service.updated);
    }

    @Test
    void printAndCompleteProcessing_withExistingConfirmationCompletesWithoutReprint() {
        order.setPrintCount(1);
        order.setPrintStatus(1);
        service.atomicCommand = false;

        PrintResultVO result = service.printAndCompleteProcessing(order.getUuid(), new PrintDTO());

        assertEquals(3, order.getOrderStatus());
        assertEquals(1, order.getPrintCount());
        assertEquals(3, result.getOrderStatus());
    }

    @Test
    void printAndCompleteProcessing_isAtomicAndRetryable() {
        service.atomicCommand = true;
        PrintResultVO first = service.printAndCompleteProcessing(order.getUuid(), new PrintDTO());

        assertEquals(3, order.getOrderStatus());
        assertEquals(3, first.getOrderStatus());
        assertEquals(1, order.getPrintCount());

        PrintResultVO retry = service.printAndCompleteProcessing(order.getUuid(), new PrintDTO());

        assertEquals(3, retry.getOrderStatus());
        assertEquals(1, order.getPrintCount());
    }

    private static final class TrackingService extends ProcessOrderServiceImpl {
        private final ProcessOrder order;
        private boolean updated;
        private boolean atomicCommand;

        TrackingService(ProcessOrder order) {
            super(mock(OriginalRollMapper.class), mock(FinishRollMapper.class), mock(ProcessStepMapper.class),
                    mock(ProcessParamMapper.class), mock(ProcessStageInputRelMapper.class),
                    mock(ProcessStageOutputMapper.class), mock(FinishOriginalRelMapper.class),
                    mock(DeliveryDetailMapper.class), mock(SettleDetailMapper.class),
                    mock(CustomerService.class), mock(OperationLogService.class), new ObjectMapper(),
                    mock(DamageImageService.class), mock(RollNoSequenceService.class), new SawPlanPreviewer(),
                    mock(DocumentNoService.class), mock(BusinessLockService.class), mock(MachineMapper.class),
                    mock(WeightCheckThresholdService.class), null, null, null, null, new BackRecordScopeResolver(),
                    null, mock(BackRecordWarehousePolicy.class), null, null, null, null, null,
                    mock(com.paper.mes.processorder.service.ProcessStepRouteMutationGuard.class),
                    null, null, mock(ProcessRouteCleanupService.class),
                    new com.paper.mes.processorder.service.ProcessOrderSettlementPolicy(),
                    mock(InventoryLedgerBusinessRecorder.class));
            this.order = order;
        }

        @Override
        public ProcessOrder getById(java.io.Serializable id) {
            return order;
        }

        @Override
        public boolean updateById(ProcessOrder entity) {
            updated = true;
            return true;
        }

        @Override
        public PrintResultVO print(String uuid, PrintDTO dto) {
            if (!atomicCommand) {
                return super.print(uuid, dto);
            }
            order.setPrintCount(1);
            order.setPrintStatus(1);
            PrintResultVO result = new PrintResultVO();
            result.setOrderStatus(2);
            return result;
        }

        @Override
        public void completeProcessing(String uuid, String reason) {
            if (!atomicCommand) {
                super.completeProcessing(uuid, reason);
                return;
            }
            order.setOrderStatus(3);
        }
    }
}
