package com.paper.mes.delivery.service;

import com.paper.mes.delivery.entity.DeliveryOrder;

import java.util.List;

/** Single source of truth for delivery detail state and available actions. */
public final class DeliveryOrderStateResolver {

    public static final int PENDING = 1;
    public static final int DELIVERED = 2;
    public static final int VOID = 3;

    private DeliveryOrderStateResolver() {
    }

    public static State resolve(Integer deliveryStatus) {
        if (deliveryStatus != null && deliveryStatus == PENDING) {
            return new State("PENDING", "LOCKED", true, true);
        }
        if (deliveryStatus != null && deliveryStatus == DELIVERED) {
            return new State("SIGNED", "DEDUCTED", false, false);
        }
        if (deliveryStatus != null && deliveryStatus == VOID) {
            return new State("NOT_REQUIRED", "RELEASED", false, false);
        }
        return new State("UNKNOWN", "UNKNOWN", false, false);
    }

    public static void enrich(DeliveryOrder order) {
        if (order == null) {
            return;
        }
        State state = resolve(order.getDeliveryStatus());
        order.setSignState(state.signState());
        order.setStockState(state.stockState());
        order.setCanSign(state.canSign());
        order.setCanEdit(state.canEdit());
    }

    public static void enrich(List<DeliveryOrder> orders) {
        if (orders == null) {
            return;
        }
        orders.forEach(DeliveryOrderStateResolver::enrich);
    }

    public record State(String signState, String stockState, boolean canSign, boolean canEdit) {
    }
}
