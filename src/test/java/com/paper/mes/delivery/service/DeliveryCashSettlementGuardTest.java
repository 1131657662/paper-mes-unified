package com.paper.mes.delivery.service;

import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeliveryCashSettlementGuardTest {

    @Test
    void hasUnsettledCashOrders_whenOrderHasNoSettleDetail_returnsTrue() {
        DeliveryCashSettlementGuard guard = guard(List.of(), List.of());

        assertTrue(guard.hasUnsettledCashOrders(orderUuids("order-a")));
    }

    @Test
    void hasUnsettledCashOrders_whenSettleOrderIsPending_returnsTrue() {
        DeliveryCashSettlementGuard guard = guard(
                List.of(detail("order-a", "settle-a")),
                List.of(settle("settle-a", 1)));

        assertTrue(guard.hasUnsettledCashOrders(orderUuids("order-a")));
    }

    @Test
    void hasUnsettledCashOrders_whenSettleOrderIsCleared_returnsFalse() {
        DeliveryCashSettlementGuard guard = guard(
                List.of(detail("order-a", "settle-a")),
                List.of(settle("settle-a", 3)));

        assertFalse(guard.hasUnsettledCashOrders(orderUuids("order-a")));
    }

    @Test
    void unsettledCashOrderUuids_whenOnlySomeOrdersCleared_returnsOnlyRiskOrders() {
        DeliveryCashSettlementGuard guard = guard(
                List.of(detail("order-a", "settle-a"), detail("order-b", "settle-b")),
                List.of(settle("settle-a", 3), settle("settle-b", 2)));

        Set<String> unsettled = guard.unsettledCashOrderUuids(orderUuids("order-a", "order-b", "order-c"));

        assertEquals(orderUuids("order-b", "order-c"), unsettled);
    }

    @Test
    void hasUnsettledCashOrders_whenSettleOrderMissing_returnsTrue() {
        DeliveryCashSettlementGuard guard = guard(List.of(detail("order-a", "settle-a")), List.of());

        assertTrue(guard.hasUnsettledCashOrders(orderUuids("order-a")));
    }

    private DeliveryCashSettlementGuard guard(List<SettleDetail> details, List<SettleOrder> orders) {
        SettleDetailMapper detailMapper = mock(SettleDetailMapper.class);
        SettleOrderMapper orderMapper = mock(SettleOrderMapper.class);
        when(detailMapper.selectList(any())).thenReturn(details);
        when(orderMapper.selectList(any())).thenReturn(orders);
        return new DeliveryCashSettlementGuard(detailMapper, orderMapper);
    }

    private static SettleDetail detail(String orderUuid, String settleUuid) {
        SettleDetail detail = new SettleDetail();
        detail.setOrderUuid(orderUuid);
        detail.setSettleUuid(settleUuid);
        return detail;
    }

    private static SettleOrder settle(String uuid, int status) {
        SettleOrder order = new SettleOrder();
        order.setUuid(uuid);
        order.setSettleStatus(status);
        return order;
    }

    private static Set<String> orderUuids(String... values) {
        return new LinkedHashSet<>(List.of(values));
    }
}
