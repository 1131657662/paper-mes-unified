package com.paper.mes.customer.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerBusinessReferenceGuardTest {

    private ProcessOrderMapper processOrderMapper;
    private DeliveryOrderMapper deliveryOrderMapper;
    private SettleOrderMapper settleOrderMapper;
    private CustomerBusinessReferenceGuard guard;

    @BeforeEach
    void setUp() {
        processOrderMapper = mock(ProcessOrderMapper.class);
        deliveryOrderMapper = mock(DeliveryOrderMapper.class);
        settleOrderMapper = mock(SettleOrderMapper.class);
        guard = new CustomerBusinessReferenceGuard(processOrderMapper, deliveryOrderMapper, settleOrderMapper);
    }

    @Test
    void requireDeletable_whenCustomerHasNoBusinessDocuments_allowsDeletion() {
        assertDoesNotThrow(() -> guard.requireDeletable("customer-1"));
    }

    @Test
    void requireDeletable_whenCustomerHasProcessOrder_rejectsDeletion() {
        when(processOrderMapper.selectOne(any())).thenReturn(new ProcessOrder());

        assertThrows(BusinessException.class, () -> guard.requireDeletable("customer-1"));
    }

    @Test
    void requireDeletable_whenCustomerHasDeliveryOrder_rejectsDeletion() {
        when(deliveryOrderMapper.selectOne(any())).thenReturn(new DeliveryOrder());

        assertThrows(BusinessException.class, () -> guard.requireDeletable("customer-1"));
    }

    @Test
    void requireDeletable_whenCustomerHasSettleOrder_rejectsDeletion() {
        when(settleOrderMapper.selectOne(any())).thenReturn(new SettleOrder());

        assertThrows(BusinessException.class, () -> guard.requireDeletable("customer-1"));
    }
}
