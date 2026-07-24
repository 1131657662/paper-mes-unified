package com.paper.mes.delivery.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import com.paper.mes.delivery.service.DeliveryCashSettlementGuard;
import com.paper.mes.delivery.service.DeliveryCustomerRevisionSnapshotWriter;
import com.paper.mes.delivery.service.DeliverySettlementBlockPolicy;
import com.paper.mes.delivery.service.DeliverySourceLockService;
import com.paper.mes.delivery.service.DeliveryWarehousePolicy;
import com.paper.mes.delivery.service.AvailableFinishSourceLoader;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.oplog.mapper.OperationLogMapper;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.system.config.service.DocumentNoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplConfirmAuthorizationTest {

    @Mock private DeliveryOrderMapper deliveryOrderMapper;
    @Mock private DeliveryDetailMapper deliveryDetailMapper;
    @Mock private FinishRollMapper finishRollMapper;
    @Mock private AvailableFinishSourceLoader availableFinishSourceLoader;
    @Mock private FinishOriginalRelMapper finishOriginalRelMapper;
    @Mock private OriginalRollMapper originalRollMapper;
    @Mock private ProcessOrderMapper processOrderMapper;
    @Mock private ProcessStepMapper processStepMapper;
    @Mock private SettleDetailMapper settleDetailMapper;
    @Mock private MachineMapper machineMapper;
    @Mock private CustomerService customerService;
    @Mock private DeliveryCashSettlementGuard cashSettlementGuard;
    @Mock private DeliverySettlementBlockPolicy settlementBlockPolicy;
    @Mock private DeliveryWarehousePolicy warehousePolicy;
    @Mock private OperationLogMapper operationLogMapper;
    @Mock private OperationLogService operationLogService;
    @Mock private DocumentNoService documentNoService;
    @Mock private BusinessLockService businessLockService;
    @Mock private DeliverySourceLockService deliverySourceLockService;
    @Mock private DeliveryCustomerRevisionSnapshotWriter customerRevisionSnapshotWriter;
    @Spy private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @InjectMocks private DeliveryServiceImpl service;

    private DeliveryOrder delivery;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, FinishRoll.class);
        TableInfoHelper.initTableInfo(assistant, DeliveryDetail.class);
        TableInfoHelper.initTableInfo(assistant, DeliveryOrder.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", deliveryOrderMapper);
        delivery = delivery();
        DeliveryDetail detail = detail();
        FinishRoll finish = finish();
        ProcessOrder order = processOrder();
        when(deliveryOrderMapper.selectById("delivery-1")).thenReturn(delivery);
        when(deliveryDetailMapper.selectList(any())).thenReturn(List.of(detail));
        when(deliverySourceLockService.lockAndReload(List.of("finish-1"))).thenReturn(
                new DeliverySourceLockService.LockedSources(
                        Map.of(finish.getUuid(), finish), Map.of(order.getUuid(), order)));
        lenient().when(finishRollMapper.selectBatchIds(any())).thenReturn(List.of(finish));
        lenient().when(processOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));
        lenient().when(finishOriginalRelMapper.selectList(any())).thenReturn(List.of());
        lenient().when(cashSettlementGuard.hasUnsettledCashOrders(any())).thenReturn(true);
    }

    @Test
    void confirm_whenCashRiskIsNotAuthorized_doesNotWriteInventory() {
        when(settlementBlockPolicy.resolveReleaseAction(true, false, "确认出库"))
                .thenThrow(new BusinessException(ErrorCode.E010));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.confirm("delivery-1", new DeliveryConfirmDTO()));

        assertEquals(ErrorCode.E010.getCode(), error.getErrorCode());
        verify(finishRollMapper, never()).update(any(), any());
        verify(deliveryDetailMapper, never()).update(any(), any());
        verify(deliveryOrderMapper, never()).update(any(), any());
        verify(customerRevisionSnapshotWriter, never()).freezeOnConfirm(any(), any(), any());
    }

    @Test
    void confirm_whenCashRiskIsAuthorized_writesInventoryAndReleaseLog() {
        DeliveryConfirmDTO dto = new DeliveryConfirmDTO();
        dto.setForceRelease(true);
        when(settlementBlockPolicy.resolveReleaseAction(true, true, "确认出库"))
                .thenReturn(DeliverySettlementBlockPolicy.ACTION_RELEASE);
        when(finishRollMapper.update(any(), any())).thenReturn(1);
        when(deliveryDetailMapper.update(any(), any())).thenReturn(1);
        when(deliveryOrderMapper.update(any(), any())).thenReturn(1);

        service.confirm("delivery-1", dto);

        assertEquals(2, delivery.getDeliveryStatus());
        assertEquals(DeliverySettlementBlockPolicy.ACTION_RELEASE, delivery.getSettleBlockAction());
        verify(finishRollMapper).update(any(), any());
        verify(customerRevisionSnapshotWriter).freezeOnConfirm(eq(delivery), any(), any());
        verify(operationLogService).record(eq(OperationLogService.BIZ_TYPE_DELIVERY),
                eq("delivery-1"), eq("CK202607240001"),
                eq(OperationLogService.ACTION_DELIVERY_RELEASE), eq(null), any());
    }

    private DeliveryOrder delivery() {
        DeliveryOrder order = new DeliveryOrder();
        order.setUuid("delivery-1");
        order.setDeliveryNo("CK202607240001");
        order.setDeliveryStatus(1);
        order.setSettleBlockAction(0);
        return order;
    }

    private DeliveryDetail detail() {
        DeliveryDetail detail = new DeliveryDetail();
        detail.setUuid("detail-1");
        detail.setDeliveryUuid("delivery-1");
        detail.setFinishUuid("finish-1");
        detail.setOrderUuid("order-1");
        detail.setFinishRollNo("P000001");
        detail.setPaperName("测试纸");
        detail.setOutWeight(new BigDecimal("10.000"));
        detail.setStockLockStatus(1);
        return detail;
    }

    private FinishRoll finish() {
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setOrderUuid("order-1");
        finish.setFinishStatus(2);
        finish.setRemainingWeight(new BigDecimal("10.000"));
        return finish;
    }

    private ProcessOrder processOrder() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(4);
        order.setSettleType(1);
        return order;
    }
}
