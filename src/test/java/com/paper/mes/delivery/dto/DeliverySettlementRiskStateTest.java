package com.paper.mes.delivery.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliverySettlementRiskStateTest {

    @Test
    void fromUnsettledCashMapsRiskToExplicitState() {
        assertEquals(
                DeliverySettlementRiskState.UNSETTLED_CASH,
                DeliverySettlementRiskState.fromUnsettledCash(true));
    }

    @Test
    void fromUnsettledCashMapsNoRiskToNone() {
        assertEquals(
                DeliverySettlementRiskState.NONE,
                DeliverySettlementRiskState.fromUnsettledCash(false));
    }
}
