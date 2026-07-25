package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.processorder.dto.OrderSettlementMode;
import com.paper.mes.processorder.dto.OrderSettlementSelection;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

/** Resolves and audits the settlement snapshot used by a process order. */
@Component
public class ProcessOrderSettlementPolicy {

    private static final int SETTLE_MONTHLY = 2;

    public void applySelection(ProcessOrder order, OrderSettlementSelection selection, Customer customer) {
        assertCustomerVersion(selection, customer);
        OrderSettlementMode mode = selection.getSettleMode() == null
                ? OrderSettlementMode.INHERIT
                : selection.getSettleMode();
        if (mode == OrderSettlementMode.INHERIT) {
            applyInherited(order, customer);
            return;
        }
        applyOverride(order, selection, customer);
    }

    public void assertCustomerVersionAtSubmit(ProcessOrder order, Customer customer) {
        if (!StringUtils.hasText(order.getSettleSource())) {
            return;
        }
        if (!Objects.equals(order.getSettleCustomerVersion(), customer.getVersion())) {
            throw staleCustomerProfile();
        }
    }

    private void assertCustomerVersion(OrderSettlementSelection selection, Customer customer) {
        Integer suppliedVersion = selection.getCustomerVersion();
        if (selection.getSettleMode() != null && suppliedVersion == null) {
            throw new BusinessException("缺少客户资料版本，请刷新客户后重试");
        }
        if (suppliedVersion != null && !Objects.equals(suppliedVersion, customer.getVersion())) {
            throw staleCustomerProfile();
        }
    }

    private void applyInherited(ProcessOrder order, Customer customer) {
        Integer settleType = customer.getSettleType() == null ? SETTLE_MONTHLY : customer.getSettleType();
        order.setSettleType(settleType);
        order.setSettleDay(settleType == SETTLE_MONTHLY ? customer.getSettleDay() : null);
        order.setSettleSource(OrderSettlementMode.INHERIT.name());
        order.setSettleOverrideReason(null);
        order.setSettleCustomerVersion(customer.getVersion());
    }

    private void applyOverride(ProcessOrder order, OrderSettlementSelection selection, Customer customer) {
        if (selection.getSettleType() == null) {
            throw new BusinessException("本单覆盖结算方式时必须选择结算方式");
        }
        if (!StringUtils.hasText(selection.getSettleOverrideReason())) {
            throw new BusinessException("本单覆盖客户结算方式时必须填写原因");
        }
        order.setSettleType(selection.getSettleType());
        order.setSettleDay(selection.getSettleType() == SETTLE_MONTHLY ? selection.getSettleDay() : null);
        order.setSettleSource(OrderSettlementMode.OVERRIDE.name());
        order.setSettleOverrideReason(selection.getSettleOverrideReason().trim());
        order.setSettleCustomerVersion(customer.getVersion());
    }

    private BusinessException staleCustomerProfile() {
        return new BusinessException("客户结算资料已更新，请返回基础信息刷新后重试");
    }
}
