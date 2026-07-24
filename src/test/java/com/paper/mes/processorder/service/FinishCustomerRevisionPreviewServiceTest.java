package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishCustomerRevisionMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FinishCustomerRevisionPreviewServiceTest {

    @Test
    void currentRejectsVoidedOrderUsingCanonicalStatusCode() {
        ProcessOrderMapper orderMapper = mock(ProcessOrderMapper.class);
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(6);
        when(orderMapper.selectById("order-1")).thenReturn(order);
        FinishCustomerRevisionPreviewService service = new FinishCustomerRevisionPreviewService(
                orderMapper,
                mock(FinishRollMapper.class),
                mock(FinishCustomerRevisionMapper.class),
                mock(FinishCustomerSpecPlanner.class));

        assertThatThrownBy(() -> service.current("order-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已作废加工单");
    }
}
