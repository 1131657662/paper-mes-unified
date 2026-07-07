package com.paper.mes.delivery.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import com.paper.mes.delivery.service.DeliveryCashSettlementGuard;
import com.paper.mes.delivery.service.DeliveryExportService;
import com.paper.mes.delivery.service.DeliverySettlementBlockPolicy;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.oplog.mapper.OperationLogMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.service.DocumentNoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplBatchLoadTest {

    @Mock private DeliveryOrderMapper deliveryOrderMapper;
    @Mock private DeliveryDetailMapper deliveryDetailMapper;
    @Mock private FinishRollMapper finishRollMapper;
    @Mock private FinishOriginalRelMapper finishOriginalRelMapper;
    @Mock private OriginalRollMapper originalRollMapper;
    @Mock private ProcessOrderMapper processOrderMapper;
    @Mock private ProcessStepMapper processStepMapper;
    @Mock private SettleDetailMapper settleDetailMapper;
    @Mock private MachineMapper machineMapper;
    @Mock private CustomerService customerService;
    @Mock private DeliveryCashSettlementGuard cashSettlementGuard;
    @Mock private DeliverySettlementBlockPolicy settlementBlockPolicy;
    @Mock private DeliveryExportService deliveryExportService;
    @Mock private OperationLogMapper operationLogMapper;
    @Mock private OperationLogService operationLogService;
    @Mock private DocumentNoService documentNoService;
    @Mock private BusinessLockService businessLockService;

    private DeliveryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeliveryServiceImpl(deliveryDetailMapper, finishRollMapper,
                finishOriginalRelMapper, originalRollMapper, processOrderMapper, processStepMapper,
                settleDetailMapper, machineMapper, customerService, cashSettlementGuard,
                settlementBlockPolicy, deliveryExportService, operationLogMapper, operationLogService,
                documentNoService, businessLockService, new ObjectMapper());
        ReflectionTestUtils.setField(service, "baseMapper", deliveryOrderMapper);
    }

    @Test
    void create_withMultipleItems_batchLoadsFinishesAndOrders() {
        when(customerService.getById("customer-1")).thenReturn(customer());
        when(deliveryDetailMapper.selectList(any())).thenReturn(List.of());
        when(finishRollMapper.selectBatchIds(any())).thenReturn(List.of(
                finish("finish-1", "order-1", "P000001"),
                finish("finish-2", "order-2", "P000002")));
        when(processOrderMapper.selectBatchIds(any())).thenReturn(List.of(
                order("order-1", "customer-1"),
                order("order-2", "customer-1")));
        when(cashSettlementGuard.hasUnsettledCashOrders(any())).thenReturn(false);
        when(settlementBlockPolicy.resolveAction(anyBoolean(), anyBoolean(), eq("出库")))
                .thenReturn(DeliverySettlementBlockPolicy.ACTION_NONE);
        when(documentNoService.next(eq(NoRuleBizType.DELIVERY_ORDER), any(LocalDate.class)))
                .thenReturn("CK202607070001");
        when(deliveryOrderMapper.insert(any(DeliveryOrder.class))).thenAnswer(invocation -> {
            DeliveryOrder order = invocation.getArgument(0);
            order.setUuid("delivery-1");
            return 1;
        });
        when(deliveryDetailMapper.insert(any(DeliveryDetail.class))).thenReturn(1);

        String uuid = service.create(createDto());

        assertEquals("delivery-1", uuid);
        verify(finishRollMapper).selectBatchIds(argThat(ids -> containsAll(ids, "finish-1", "finish-2")));
        verify(processOrderMapper).selectBatchIds(argThat(ids -> containsAll(ids, "order-1", "order-2")));
        verify(finishRollMapper, never()).selectById(any());
        verify(processOrderMapper, never()).selectById(any());
    }

    private DeliveryCreateDTO createDto() {
        DeliveryCreateDTO dto = new DeliveryCreateDTO();
        dto.setCustomerUuid("customer-1");
        dto.setDeliveryDate(LocalDate.of(2026, 7, 7));
        dto.setItems(List.of(item("finish-1", "10.00"), item("finish-2", "12.50")));
        return dto;
    }

    private DeliveryCreateDTO.Item item(String finishUuid, String outWeight) {
        DeliveryCreateDTO.Item item = new DeliveryCreateDTO.Item();
        item.setFinishUuid(finishUuid);
        item.setOutWeight(new BigDecimal(outWeight));
        return item;
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setUuid("customer-1");
        customer.setCustomerName("测试客户");
        return customer;
    }

    private FinishRoll finish(String uuid, String orderUuid, String rollNo) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setOrderUuid(orderUuid);
        finish.setFinishRollNo(rollNo);
        finish.setFinishStatus(2);
        finish.setActualWeight(new BigDecimal("50.00"));
        finish.setRemainingWeight(new BigDecimal("50.00"));
        return finish;
    }

    private ProcessOrder order(String uuid, String customerUuid) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid(uuid);
        order.setCustomerUuid(customerUuid);
        order.setOrderNo("JG-" + uuid);
        order.setOrderStatus(4);
        return order;
    }

    private boolean containsAll(Collection<?> values, String first, String second) {
        return values.size() == 2 && values.contains(first) && values.contains(second);
    }
}
