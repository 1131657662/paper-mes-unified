package com.paper.mes.settle.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettleReceiveStatusResolverTest {

    @Test
    void resolve_whenTotalIsZero_marksCleared() {
        SettleReceiveStatusResolver.State state =
                SettleReceiveStatusResolver.resolve(BigDecimal.ZERO, BigDecimal.ZERO);

        assertEquals(SettleReceiveStatusResolver.STATUS_CLEARED, state.status());
        assertEquals(new BigDecimal("0.00"), state.unreceivedAmount());
    }

    @Test
    void resolve_whenNothingReceived_marksPending() {
        SettleReceiveStatusResolver.State state =
                SettleReceiveStatusResolver.resolve(new BigDecimal("100.00"), BigDecimal.ZERO);

        assertEquals(SettleReceiveStatusResolver.STATUS_PENDING, state.status());
        assertEquals(new BigDecimal("100.00"), state.unreceivedAmount());
    }

    @Test
    void resolve_whenPartiallyReceived_marksPartial() {
        SettleReceiveStatusResolver.State state =
                SettleReceiveStatusResolver.resolve(new BigDecimal("100.00"), new BigDecimal("40.00"));

        assertEquals(SettleReceiveStatusResolver.STATUS_PARTIAL, state.status());
        assertEquals(new BigDecimal("60.00"), state.unreceivedAmount());
    }

    @Test
    void resolve_whenFullyReceived_marksCleared() {
        SettleReceiveStatusResolver.State state =
                SettleReceiveStatusResolver.resolve(new BigDecimal("100.00"), new BigDecimal("100.00"));

        assertEquals(SettleReceiveStatusResolver.STATUS_CLEARED, state.status());
        assertEquals(new BigDecimal("0.00"), state.unreceivedAmount());
    }
}
