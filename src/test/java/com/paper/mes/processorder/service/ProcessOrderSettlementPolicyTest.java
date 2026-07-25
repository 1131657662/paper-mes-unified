package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.processorder.dto.DraftOrderBaseDTO;
import com.paper.mes.processorder.dto.OrderSettlementMode;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ProcessOrderSettlementPolicyTest {

    private final ProcessOrderSettlementPolicy policy = new ProcessOrderSettlementPolicy();

    @Test
    void applySelection_whenInherited_usesCurrentCustomerSnapshot() {
        DraftOrderBaseDTO selection = selection(OrderSettlementMode.INHERIT, 4);
        selection.setSettleType(2);
        ProcessOrder order = new ProcessOrder();

        policy.applySelection(order, selection, customer(4, 1, null));

        assertEquals(1, order.getSettleType());
        assertNull(order.getSettleDay());
        assertEquals("INHERIT", order.getSettleSource());
        assertEquals(4, order.getSettleCustomerVersion());
    }

    @Test
    void applySelection_whenOverride_persistsReasonAndSelectedTerms() {
        DraftOrderBaseDTO selection = selection(OrderSettlementMode.OVERRIDE, 4);
        selection.setSettleType(2);
        selection.setSettleDay(25);
        selection.setSettleOverrideReason("  合同约定  ");
        ProcessOrder order = new ProcessOrder();

        policy.applySelection(order, selection, customer(4, 1, null));

        assertEquals(2, order.getSettleType());
        assertEquals(25, order.getSettleDay());
        assertEquals("OVERRIDE", order.getSettleSource());
        assertEquals("合同约定", order.getSettleOverrideReason());
    }

    @Test
    void applySelection_whenOverrideReasonMissing_rejectsRequest() {
        DraftOrderBaseDTO selection = selection(OrderSettlementMode.OVERRIDE, 4);
        selection.setSettleType(2);

        BusinessException error = assertThrows(BusinessException.class,
                () -> policy.applySelection(new ProcessOrder(), selection, customer(4, 1, null)));

        assertEquals("本单覆盖客户结算方式时必须填写原因", error.getMessage());
    }

    @Test
    void applySelection_whenCustomerVersionChanged_rejectsStalePage() {
        DraftOrderBaseDTO selection = selection(OrderSettlementMode.INHERIT, 3);

        BusinessException error = assertThrows(BusinessException.class,
                () -> policy.applySelection(new ProcessOrder(), selection, customer(4, 1, null)));

        assertEquals("客户结算资料已更新，请返回基础信息刷新后重试", error.getMessage());
    }

    @Test
    void assertCustomerVersionAtSubmit_whenHistoricalSourceUnknown_keepsLegacySnapshot() {
        ProcessOrder order = new ProcessOrder();
        order.setSettleType(2);

        assertDoesNotThrow(() -> policy.assertCustomerVersionAtSubmit(order, customer(5, 1, null)));
    }

    @Test
    void assertCustomerVersionAtSubmit_whenCustomerChanged_rejectsSubmission() {
        ProcessOrder order = new ProcessOrder();
        order.setSettleSource("INHERIT");
        order.setSettleCustomerVersion(4);

        assertThrows(BusinessException.class,
                () -> policy.assertCustomerVersionAtSubmit(order, customer(5, 1, null)));
    }

    private DraftOrderBaseDTO selection(OrderSettlementMode mode, int customerVersion) {
        DraftOrderBaseDTO selection = new DraftOrderBaseDTO();
        selection.setSettleMode(mode);
        selection.setCustomerVersion(customerVersion);
        return selection;
    }

    private Customer customer(int version, int settleType, Integer settleDay) {
        Customer customer = new Customer();
        customer.setVersion(version);
        customer.setSettleType(settleType);
        customer.setSettleDay(settleDay);
        return customer;
    }
}
