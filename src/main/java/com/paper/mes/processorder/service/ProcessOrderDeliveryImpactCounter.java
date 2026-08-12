package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Counts editable delivery documents which still reflect the current customer presentation. */
@Component
@RequiredArgsConstructor
public class ProcessOrderDeliveryImpactCounter {

    private static final int DELIVERY_STATUS_PENDING = 1;
    private static final int DELIVERY_STATUS_CONFIRMED = 2;

    private final DeliveryDetailMapper detailMapper;
    private final DeliveryOrderMapper orderMapper;

    public int pendingDeliveryCount(String processOrderUuid) {
        return (int) deliveryOrders(processOrderUuid).stream()
                .filter(order -> Integer.valueOf(DELIVERY_STATUS_PENDING).equals(order.getDeliveryStatus()))
                .count();
    }

    public boolean hasConfirmedDelivery(String processOrderUuid) {
        return deliveryOrders(processOrderUuid).stream()
                .anyMatch(order -> Integer.valueOf(DELIVERY_STATUS_CONFIRMED).equals(order.getDeliveryStatus()));
    }

    private List<DeliveryOrder> deliveryOrders(String processOrderUuid) {
        List<String> deliveryUuids = detailMapper.selectList(new LambdaQueryWrapper<DeliveryDetail>()
                        .eq(DeliveryDetail::getOrderUuid, processOrderUuid))
                .stream()
                .map(DeliveryDetail::getDeliveryUuid)
                .distinct()
                .toList();
        return deliveryUuids.isEmpty() ? List.of() : orderMapper.selectBatchIds(deliveryUuids);
    }
}
