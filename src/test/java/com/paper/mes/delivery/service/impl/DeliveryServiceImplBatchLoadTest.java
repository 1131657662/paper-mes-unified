package com.paper.mes.delivery.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.delivery.dto.DeliveryAppendItemsDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.dto.AvailableFinishVO;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @Mock private DeliveryCustomerRevisionSnapshotWriter customerRevisionSnapshotWriter;
    @Mock private DeliverySourceLockService deliverySourceLockService;

    private DeliveryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeliveryServiceImpl(deliveryDetailMapper, finishRollMapper, availableFinishSourceLoader,
                finishOriginalRelMapper, originalRollMapper, processOrderMapper, processStepMapper,
                settleDetailMapper, machineMapper, customerService, cashSettlementGuard,
                settlementBlockPolicy, warehousePolicy,
                operationLogMapper, operationLogService,
                documentNoService, businessLockService, deliverySourceLockService,
                new ObjectMapper(), customerRevisionSnapshotWriter);
        ReflectionTestUtils.setField(service, "baseMapper", deliveryOrderMapper);
    }

    @Test
    void create_withMultipleItems_batchLoadsFinishesAndOrders() {
        when(customerService.getById("customer-1")).thenReturn(customer());
        when(deliveryDetailMapper.selectList(any())).thenReturn(List.of());
        Map<String, FinishRoll> finishes = Map.of(
                "finish-1", finish("finish-1", "order-1", "P000001"),
                "finish-2", finish("finish-2", "order-2", "P000002"));
        ProcessOrder firstOrder = order("order-1", "customer-1");
        ProcessOrder secondOrder = order("order-2", "customer-1");
        firstOrder.setSettleType(1);
        secondOrder.setSettleType(1);
        Map<String, ProcessOrder> orders = Map.of(
                "order-1", firstOrder, "order-2", secondOrder);
        when(deliverySourceLockService.lockAndReload(any()))
                .thenReturn(new DeliverySourceLockService.LockedSources(finishes, orders));
        when(cashSettlementGuard.hasUnsettledCashOrders(any())).thenReturn(true);
        when(settlementBlockPolicy.resolveAction(anyBoolean(), anyBoolean(), eq("出库")))
                .thenReturn(DeliverySettlementBlockPolicy.ACTION_RELEASE);
        when(warehousePolicy.requireForCreate(eq("warehouse-1"), any()))
                .thenReturn(new DeliveryWarehousePolicy.WarehouseSnapshot(
                        "warehouse-1", "成品一仓", "一号库区"));
        when(documentNoService.next(eq(NoRuleBizType.DELIVERY_ORDER), any(LocalDate.class)))
                .thenReturn("CK202607070001");
        when(deliveryOrderMapper.insert(any(DeliveryOrder.class))).thenAnswer(invocation -> {
            DeliveryOrder order = invocation.getArgument(0);
            order.setUuid("delivery-1");
            return 1;
        });
        when(deliveryDetailMapper.insert(any(DeliveryDetail.class))).thenReturn(1);

        DeliveryCreateDTO dto = createDto();
        dto.setForceRelease(true);
        String uuid = service.create(dto);

        assertEquals("delivery-1", uuid);
        verify(deliverySourceLockService).lockAndReload(
                argThat(ids -> containsAll(ids, "finish-1", "finish-2")));
        verify(finishRollMapper, never()).selectById(any());
        verify(processOrderMapper, never()).selectById(any());
        verify(operationLogService).record(eq(OperationLogService.BIZ_TYPE_DELIVERY),
                eq("delivery-1"), eq("CK202607070001"),
                eq(OperationLogService.ACTION_DELIVERY_CREATE), eq(null), any());
        verify(operationLogService).record(eq(OperationLogService.BIZ_TYPE_DELIVERY),
                eq("delivery-1"), eq("CK202607070001"),
                eq(OperationLogService.ACTION_DELIVERY_RISK_CONFIRM), eq(null), any());
        verify(operationLogService, never()).record(any(), any(), any(),
                eq(OperationLogService.ACTION_DELIVERY_RELEASE), any(), any());
    }

    @Test
    void listAvailable_attachesBatchLoadedMotherRollSources() {
        ProcessOrder order = order("order-1", "customer-1");
        FinishRoll finish = finish("finish-1", "order-1", "P000001");
        AvailableFinishVO.SourceMotherRollVO source = new AvailableFinishVO.SourceMotherRollVO();
        source.setOriginalUuid("original-1");
        source.setRowSort(1);

        when(processOrderMapper.selectList(any())).thenReturn(List.of(order));
        when(deliveryDetailMapper.selectList(any())).thenReturn(List.of());
        when(cashSettlementGuard.unsettledCashOrderUuids(any())).thenReturn(java.util.Set.of());
        when(finishRollMapper.selectList(any())).thenReturn(List.of(finish));
        when(availableFinishSourceLoader.load(any())).thenReturn(
                java.util.Map.of("finish-1", List.of(source)));

        List<AvailableFinishVO> result = service.listAvailable("customer-1");

        assertEquals("original-1", result.getFirst().getSourceMotherRolls().getFirst().getOriginalUuid());
        verify(availableFinishSourceLoader).load(any());
    }

    @Test
    void appendDetails_withNonDeliverableProcessOrder_rejectsFinish() {
        DeliveryOrder delivery = new DeliveryOrder();
        delivery.setUuid("delivery-1");
        delivery.setCustomerUuid("customer-1");
        delivery.setDeliveryStatus(1);
        ProcessOrder processing = order("order-2", "customer-1");
        processing.setOrderStatus(3);
        when(deliveryOrderMapper.selectById("delivery-1")).thenReturn(delivery);
        when(deliveryDetailMapper.selectList(any())).thenReturn(List.of());
        FinishRoll finish = finish("finish-2", "order-2", "P000002");
        when(deliverySourceLockService.lockAndReload(any())).thenReturn(
                new DeliverySourceLockService.LockedSources(
                        Map.of(finish.getUuid(), finish), Map.of(processing.getUuid(), processing)));

        assertThrows(BusinessException.class,
                () -> service.appendDetails("delivery-1", appendDto()));

        verify(deliveryDetailMapper, never()).insert(any(DeliveryDetail.class));
    }

    private DeliveryCreateDTO createDto() {
        DeliveryCreateDTO dto = new DeliveryCreateDTO();
        dto.setCustomerUuid("customer-1");
        dto.setWarehouseUuid("warehouse-1");
        dto.setDeliveryDate(LocalDate.of(2026, 7, 7));
        dto.setItems(List.of(item("finish-1", "10.00"), item("finish-2", "12.50")));
        return dto;
    }

    private DeliveryAppendItemsDTO appendDto() {
        DeliveryAppendItemsDTO.Item item = new DeliveryAppendItemsDTO.Item();
        item.setFinishUuid("finish-2");
        item.setOutWeight(new BigDecimal("12.50"));
        DeliveryAppendItemsDTO dto = new DeliveryAppendItemsDTO();
        dto.setItems(List.of(item));
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
        finish.setWarehouseUuid("warehouse-1");
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
