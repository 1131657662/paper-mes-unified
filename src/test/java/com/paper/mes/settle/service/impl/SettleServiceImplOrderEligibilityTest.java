package com.paper.mes.settle.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettleServiceImplOrderEligibilityTest {

    @Test
    void validateFinishedOrder_whenFinished_allowsSettlement() {
        assertThatCode(() -> SettleServiceImpl.validateFinishedOrder(order(4)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateFinishedOrder_whenSettled_reportsDuplicateSettlement() {
        assertThatThrownBy(() -> SettleServiceImpl.validateFinishedOrder(order(5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已结算")
                .hasMessageContaining("不可重复结算");
    }

    @Test
    void validateFinishedOrder_whenVoided_reportsVoidedState() {
        assertThatThrownBy(() -> SettleServiceImpl.validateFinishedOrder(order(6)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已作废");
    }

    private ProcessOrder order(int status) {
        ProcessOrder order = new ProcessOrder();
        order.setOrderNo("JG-TEST-001");
        order.setOrderStatus(status);
        return order;
    }
}
