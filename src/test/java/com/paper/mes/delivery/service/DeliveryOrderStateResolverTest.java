package com.paper.mes.delivery.service;

import com.paper.mes.delivery.entity.DeliveryOrder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryOrderStateResolverTest {

    @Test
    void voidOrderDoesNotRequireSignAndHasReleasedStock() {
        DeliveryOrder order = new DeliveryOrder();
        order.setDeliveryStatus(DeliveryOrderStateResolver.VOID);

        DeliveryOrderStateResolver.enrich(order);

        assertThat(order.getSignState()).isEqualTo("NOT_REQUIRED");
        assertThat(order.getStockState()).isEqualTo("RELEASED");
        assertThat(order.getCanSign()).isFalse();
        assertThat(order.getCanEdit()).isFalse();
    }

    @Test
    void listEnrichmentUsesTheSameStateRules() {
        DeliveryOrder pending = new DeliveryOrder();
        pending.setDeliveryStatus(DeliveryOrderStateResolver.PENDING);
        DeliveryOrder delivered = new DeliveryOrder();
        delivered.setDeliveryStatus(DeliveryOrderStateResolver.DELIVERED);

        DeliveryOrderStateResolver.enrich(List.of(pending, delivered));

        assertThat(pending.getSignState()).isEqualTo("PENDING");
        assertThat(pending.getStockState()).isEqualTo("LOCKED");
        assertThat(delivered.getSignState()).isEqualTo("SIGNED");
        assertThat(delivered.getStockState()).isEqualTo("DEDUCTED");
    }
}
