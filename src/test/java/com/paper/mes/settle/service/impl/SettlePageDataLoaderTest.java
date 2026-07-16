package com.paper.mes.settle.service.impl;

import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.ReceiveRecordMapper;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettlePageDataLoaderTest {

    @Test
    void load_withMultipleSettlements_queriesEachRelatedTableOnce() {
        SettleDetailMapper detailMapper = mock(SettleDetailMapper.class);
        ReceiveRecordMapper receiveMapper = mock(ReceiveRecordMapper.class);
        ProcessOrderService orderService = mock(ProcessOrderService.class);
        CustomerService customerService = mock(CustomerService.class);
        when(detailMapper.selectList(any())).thenReturn(List.of(
                detail("settle-1", "order-1"), detail("settle-2", "order-2")));
        when(receiveMapper.selectList(any())).thenReturn(List.of(
                receive("settle-1", "30"), receive("settle-1", "20")));
        when(orderService.listByIds(any())).thenReturn(List.of(order("order-1"), order("order-2")));
        when(customerService.listByIds(any())).thenReturn(List.of(customer("customer-1"), customer("customer-2")));
        SettlePageDataLoader loader = new SettlePageDataLoader(
                detailMapper, receiveMapper, orderService, customerService);

        SettlePageDataLoader.PageData data = loader.load(List.of(
                settlement("settle-1", "customer-1"), settlement("settle-2", "customer-2")));

        assertEquals(2, data.detailsBySettle().size());
        assertEquals(2, data.orderByUuid().size());
        assertEquals(2, data.customerByUuid().size());
        assertEquals(0, new BigDecimal("50.00").compareTo(
                data.receiveTotalsBySettle().get("settle-1").receiveAmount()));
        assertEquals(0, new BigDecimal("2.00").compareTo(
                data.receiveTotalsBySettle().get("settle-1").discountAmount()));
        verify(detailMapper).selectList(any());
        verify(receiveMapper).selectList(any());
        verify(orderService).listByIds(any());
        verify(customerService).listByIds(any());
    }

    private SettleOrder settlement(String uuid, String customerUuid) {
        SettleOrder value = new SettleOrder();
        value.setUuid(uuid);
        value.setCustomerUuid(customerUuid);
        return value;
    }

    private SettleDetail detail(String settleUuid, String orderUuid) {
        SettleDetail value = new SettleDetail();
        value.setSettleUuid(settleUuid);
        value.setOrderUuid(orderUuid);
        return value;
    }

    private ReceiveRecord receive(String settleUuid, String amount) {
        ReceiveRecord value = new ReceiveRecord();
        value.setSettleUuid(settleUuid);
        value.setRecordStatus(1);
        value.setReceiveAmount(new BigDecimal(amount));
        value.setCashAmount(new BigDecimal(amount).subtract(BigDecimal.ONE));
        value.setScrapOffsetAmount(BigDecimal.ZERO);
        value.setDiscountAmount(BigDecimal.ONE);
        return value;
    }

    private ProcessOrder order(String uuid) {
        ProcessOrder value = new ProcessOrder();
        value.setUuid(uuid);
        return value;
    }

    private Customer customer(String uuid) {
        Customer value = new Customer();
        value.setUuid(uuid);
        return value;
    }
}
