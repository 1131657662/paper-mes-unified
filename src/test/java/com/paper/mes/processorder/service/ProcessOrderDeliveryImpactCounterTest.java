package com.paper.mes.processorder.service;

import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessOrderDeliveryImpactCounterTest {

    @Test
    void pendingDelivery_countsAsImpactButDoesNotBlockReissue() {
        DeliveryDetailMapper detailMapper = mock(DeliveryDetailMapper.class);
        DeliveryOrderMapper orderMapper = mock(DeliveryOrderMapper.class);
        when(detailMapper.selectList(any())).thenReturn(List.of(detail("delivery-1")));
        when(orderMapper.selectBatchIds(List.of("delivery-1"))).thenReturn(List.of(delivery("delivery-1", 1)));
        ProcessOrderDeliveryImpactCounter counter = new ProcessOrderDeliveryImpactCounter(detailMapper, orderMapper);

        int pendingCount = counter.pendingDeliveryCount("order-1");
        boolean hasConfirmedDelivery = counter.hasConfirmedDelivery("order-1");

        assertThat(pendingCount).isEqualTo(1);
        assertThat(hasConfirmedDelivery).isFalse();
    }

    @Test
    void confirmedDelivery_blocksReissue() {
        DeliveryDetailMapper detailMapper = mock(DeliveryDetailMapper.class);
        DeliveryOrderMapper orderMapper = mock(DeliveryOrderMapper.class);
        when(detailMapper.selectList(any())).thenReturn(List.of(detail("delivery-1")));
        when(orderMapper.selectBatchIds(List.of("delivery-1"))).thenReturn(List.of(delivery("delivery-1", 2)));
        ProcessOrderDeliveryImpactCounter counter = new ProcessOrderDeliveryImpactCounter(detailMapper, orderMapper);

        boolean hasConfirmedDelivery = counter.hasConfirmedDelivery("order-1");

        assertThat(hasConfirmedDelivery).isTrue();
    }

    private DeliveryDetail detail(String deliveryUuid) {
        DeliveryDetail detail = new DeliveryDetail();
        detail.setDeliveryUuid(deliveryUuid);
        return detail;
    }

    private DeliveryOrder delivery(String uuid, int status) {
        DeliveryOrder order = new DeliveryOrder();
        order.setUuid(uuid);
        order.setDeliveryStatus(status);
        return order;
    }
}
