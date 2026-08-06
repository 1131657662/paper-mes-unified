package com.paper.mes.delivery.dto;

/** Explicit settlement risk carried by an available delivery item. */
public enum DeliverySettlementRiskState {
    NONE,
    UNSETTLED_CASH;

    public static DeliverySettlementRiskState fromUnsettledCash(boolean unsettledCash) {
        return unsettledCash ? UNSETTLED_CASH : NONE;
    }
}
