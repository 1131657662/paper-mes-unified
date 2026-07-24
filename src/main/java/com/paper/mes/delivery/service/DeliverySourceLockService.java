package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliverySourceLockService {

    private final FinishRollMapper finishRollMapper;
    private final ProcessOrderMapper processOrderMapper;
    private final BusinessLockService businessLockService;

    public LockedSources lockAndReload(Collection<String> requestedFinishUuids) {
        List<String> finishUuids = normalize(requestedFinishUuids);
        Map<String, FinishRoll> initialFinishes = loadFinishes(finishUuids);
        List<String> orderUuids = orderUuids(initialFinishes.values());
        businessLockService.lockProcessOrders(orderUuids);
        Map<String, ProcessOrder> lockedOrders = loadOrdersForUpdate(orderUuids);
        businessLockService.lockFinishRolls(finishUuids);
        Map<String, FinishRoll> lockedFinishes = loadFinishesForUpdate(finishUuids);
        if (!orderUuids.equals(orderUuids(lockedFinishes.values()))) {
            throw new BusinessException(ErrorCode.E006, "成品来源加工单已变化，请刷新后重试");
        }
        return new LockedSources(lockedFinishes, lockedOrders);
    }

    private List<String> normalize(Collection<String> uuids) {
        if (uuids == null) return List.of();
        return uuids.stream().filter(StringUtils::hasText).distinct().sorted().toList();
    }

    private Map<String, FinishRoll> loadFinishes(List<String> uuids) {
        if (uuids.isEmpty()) return Map.of();
        return finishRollMapper.selectBatchIds(uuids).stream().collect(Collectors.toMap(
                FinishRoll::getUuid, Function.identity(), (left, right) -> left));
    }

    private Map<String, FinishRoll> loadFinishesForUpdate(List<String> uuids) {
        if (uuids.isEmpty()) return Map.of();
        return finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                        .in(FinishRoll::getUuid, uuids)
                        .orderByAsc(FinishRoll::getUuid)
                        .last("FOR UPDATE"))
                .stream().collect(Collectors.toMap(
                        FinishRoll::getUuid, Function.identity(), (left, right) -> left));
    }

    private Map<String, ProcessOrder> loadOrdersForUpdate(List<String> uuids) {
        if (uuids.isEmpty()) return Map.of();
        return processOrderMapper.selectList(new LambdaQueryWrapper<ProcessOrder>()
                        .in(ProcessOrder::getUuid, uuids)
                        .orderByAsc(ProcessOrder::getUuid)
                        .last("FOR UPDATE"))
                .stream().collect(Collectors.toMap(
                        ProcessOrder::getUuid, Function.identity(), (left, right) -> left));
    }

    private List<String> orderUuids(Collection<FinishRoll> finishes) {
        return finishes.stream().map(FinishRoll::getOrderUuid)
                .filter(StringUtils::hasText).distinct().sorted().toList();
    }

    public record LockedSources(
            Map<String, FinishRoll> finishes,
            Map<String, ProcessOrder> processOrders) {
    }
}
