package com.paper.mes.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerBusinessReferenceGuard {

    private final ProcessOrderMapper processOrderMapper;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final SettleOrderMapper settleOrderMapper;

    public void requireDeletable(String customerUuid) {
        if (hasProcessOrder(customerUuid)) {
            throw new BusinessException("客户已关联加工单，不能删除");
        }
        if (hasDeliveryOrder(customerUuid)) {
            throw new BusinessException("客户已关联出库单，不能删除");
        }
        if (hasSettleOrder(customerUuid)) {
            throw new BusinessException("客户已关联结算单，不能删除");
        }
    }

    private boolean hasProcessOrder(String customerUuid) {
        return processOrderMapper.selectOne(new LambdaQueryWrapper<ProcessOrder>()
                .eq(ProcessOrder::getCustomerUuid, customerUuid).last("LIMIT 1")) != null;
    }

    private boolean hasDeliveryOrder(String customerUuid) {
        return deliveryOrderMapper.selectOne(new LambdaQueryWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getCustomerUuid, customerUuid).last("LIMIT 1")) != null;
    }

    private boolean hasSettleOrder(String customerUuid) {
        return settleOrderMapper.selectOne(new LambdaQueryWrapper<SettleOrder>()
                .eq(SettleOrder::getCustomerUuid, customerUuid).last("LIMIT 1")) != null;
    }
}
