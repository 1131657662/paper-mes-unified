package com.paper.mes.remain.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RemainApplicationTargetValidatorTest {

    private final SettleDetailMapper detailMapper = mock(SettleDetailMapper.class);
    private final RemainApplicationTargetValidator validator = new RemainApplicationTargetValidator(detailMapper);

    @Test
    void requireApplicable_withMismatchedCustomer_rejectsTarget() {
        RemainRegistration registration = registration();
        SettleOrder settle = settle();
        settle.setCustomerUuid("other-customer");

        assertThatThrownBy(() -> validator.requireApplicable(settle, registration))
                .isInstanceOf(BusinessException.class)
                .hasMessage("登记客户与结算客户不一致");
    }

    @Test
    void requireApplicable_withSettledTarget_rejectsTarget() {
        RemainRegistration registration = registration();
        SettleOrder settle = settle();
        settle.setSettleStatus(3);

        assertThatThrownBy(() -> validator.requireApplicable(settle, registration))
                .isInstanceOf(BusinessException.class)
                .hasMessage("结算单当前状态不可抵扣");
    }

    @Test
    void requireApplicable_withSourceOrderInTarget_acceptsTarget() {
        when(detailMapper.selectList(any())).thenReturn(List.of(new SettleDetail()));

        assertThatCode(() -> validator.requireApplicable(settle(), registration())).doesNotThrowAnyException();
    }

    private static RemainRegistration registration() {
        RemainRegistration registration = new RemainRegistration();
        registration.setCustomerUuid("customer-1");
        registration.setOrderUuid("order-1");
        registration.setPriceStatus("CONFIRMED");
        return registration;
    }

    private static SettleOrder settle() {
        SettleOrder settle = new SettleOrder();
        settle.setUuid("settle-1");
        settle.setCustomerUuid("customer-1");
        settle.setSettleStatus(1);
        return settle;
    }
}
