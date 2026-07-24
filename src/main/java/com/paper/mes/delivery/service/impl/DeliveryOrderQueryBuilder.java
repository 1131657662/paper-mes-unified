package com.paper.mes.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.entity.DeliveryOrder;
import org.springframework.util.StringUtils;

public final class DeliveryOrderQueryBuilder {

    private DeliveryOrderQueryBuilder() {
    }

    public static LambdaQueryWrapper<DeliveryOrder> build(DeliveryQuery query) {
        LambdaQueryWrapper<DeliveryOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(DeliveryOrder::getDeliveryNo, keyword)
                    .or().like(DeliveryOrder::getCustomerName, keyword));
        }
        if (StringUtils.hasText(query.getCustomerUuid())) {
            wrapper.eq(DeliveryOrder::getCustomerUuid, query.getCustomerUuid());
        }
        if (query.getDeliveryStatus() != null) {
            wrapper.eq(DeliveryOrder::getDeliveryStatus, query.getDeliveryStatus());
        }
        if (query.getDateFrom() != null) {
            wrapper.ge(DeliveryOrder::getDeliveryDate, query.getDateFrom());
        }
        if (query.getDateTo() != null) {
            wrapper.le(DeliveryOrder::getDeliveryDate, query.getDateTo());
        }
        return wrapper.orderByDesc(DeliveryOrder::getCreateTime)
                .orderByDesc(DeliveryOrder::getUuid);
    }
}
