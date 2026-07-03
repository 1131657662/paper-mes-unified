package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryCashSettlementGuard {

    private static final int SETTLE_STATUS_CLEARED = 3;

    private final SettleDetailMapper settleDetailMapper;
    private final SettleOrderMapper settleOrderMapper;

    public boolean hasUnsettledCashOrders(Set<String> orderUuids) {
        return !unsettledCashOrderUuids(orderUuids).isEmpty();
    }

    public Set<String> unsettledCashOrderUuids(Set<String> orderUuids) {
        Set<String> normalized = normalize(orderUuids);
        if (normalized.isEmpty()) {
            return Set.of();
        }
        List<SettleDetail> details = activeDetails(normalized);
        Map<String, Integer> statusBySettleUuid = activeSettleStatusByUuid(details);
        return normalized.stream()
                .filter(orderUuid -> !isOrderCleared(orderUuid, details, statusBySettleUuid))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> normalize(Set<String> orderUuids) {
        if (orderUuids == null || orderUuids.isEmpty()) {
            return Set.of();
        }
        return orderUuids.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<SettleDetail> activeDetails(Set<String> orderUuids) {
        return settleDetailMapper.selectList(new LambdaQueryWrapper<SettleDetail>()
                .eq(SettleDetail::getIsDeleted, 0)
                .in(SettleDetail::getOrderUuid, orderUuids));
    }

    private Map<String, Integer> activeSettleStatusByUuid(List<SettleDetail> details) {
        Set<String> settleUuids = details.stream()
                .map(SettleDetail::getSettleUuid)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (settleUuids.isEmpty()) {
            return Map.of();
        }
        return settleOrderMapper.selectList(new LambdaQueryWrapper<SettleOrder>()
                        .eq(SettleOrder::getIsDeleted, 0)
                        .in(SettleOrder::getUuid, settleUuids))
                .stream()
                .collect(Collectors.toMap(SettleOrder::getUuid, SettleOrder::getSettleStatus,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private boolean isOrderCleared(String orderUuid, List<SettleDetail> details,
                                   Map<String, Integer> statusBySettleUuid) {
        List<SettleDetail> orderDetails = details.stream()
                .filter(detail -> orderUuid.equals(detail.getOrderUuid()))
                .toList();
        if (orderDetails.isEmpty()) {
            return false;
        }
        return orderDetails.stream().allMatch(detail ->
                SETTLE_STATUS_CLEARED == statusBySettleUuid.getOrDefault(detail.getSettleUuid(), 0));
    }
}
