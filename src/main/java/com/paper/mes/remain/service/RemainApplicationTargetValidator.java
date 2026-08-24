package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class RemainApplicationTargetValidator {

    private final SettleDetailMapper settleDetailMapper;

    void requireApplicable(SettleOrder settle, RemainRegistration registration) {
        requireSameCustomer(settle, registration);
        requireConfirmedPrice(registration);
        requireReceivableSettle(settle);
        requireSourceOrderIncluded(settle, registration);
    }

    private void requireSameCustomer(SettleOrder settle, RemainRegistration registration) {
        if (!registration.getCustomerUuid().equals(settle.getCustomerUuid())) {
            throw new BusinessException("登记客户与结算客户不一致");
        }
    }

    private void requireConfirmedPrice(RemainRegistration registration) {
        if (!"CONFIRMED".equals(registration.getPriceStatus())) {
            throw new BusinessException("登记单尚未确认价格");
        }
    }

    private void requireReceivableSettle(SettleOrder settle) {
        if (settle.getSettleStatus() == null || settle.getSettleStatus() < 1 || settle.getSettleStatus() > 2) {
            throw new BusinessException("结算单当前状态不可抵扣");
        }
    }

    private void requireSourceOrderIncluded(SettleOrder settle, RemainRegistration registration) {
        boolean included = settleDetailMapper.selectList(new LambdaQueryWrapper<SettleDetail>()
                .eq(SettleDetail::getSettleUuid, settle.getUuid())
                .eq(SettleDetail::getOrderUuid, registration.getOrderUuid())).stream().findAny().isPresent();
        if (!included) {
            throw new BusinessException("登记来源加工单不在目标结算单内");
        }
    }
}
