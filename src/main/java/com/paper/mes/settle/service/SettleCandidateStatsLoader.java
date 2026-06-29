package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SettleCandidateStatsLoader {

    private static final int IS_SPARE_NO = 0;

    private final OriginalRollMapper originalRollMapper;
    private final FinishRollMapper finishRollMapper;

    public Map<String, CandidateStats> load(List<String> orderUuids) {
        if (orderUuids == null || orderUuids.isEmpty()) {
            return Map.of();
        }
        Map<String, List<OriginalRoll>> originals = loadOriginalsByOrder(orderUuids);
        Map<String, List<FinishRoll>> finishes = loadFinishesByOrder(orderUuids);
        Map<String, CandidateStats> result = new LinkedHashMap<>();
        for (String orderUuid : orderUuids) {
            result.put(orderUuid, buildStats(originals.get(orderUuid), finishes.get(orderUuid)));
        }
        return result;
    }

    private Map<String, List<OriginalRoll>> loadOriginalsByOrder(List<String> orderUuids) {
        List<OriginalRoll> rolls = originalRollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                .in(OriginalRoll::getOrderUuid, orderUuids));
        Map<String, List<OriginalRoll>> grouped = new LinkedHashMap<>();
        for (OriginalRoll roll : rolls) {
            grouped.computeIfAbsent(roll.getOrderUuid(), key -> new ArrayList<>()).add(roll);
        }
        return grouped;
    }

    private Map<String, List<FinishRoll>> loadFinishesByOrder(List<String> orderUuids) {
        List<FinishRoll> rolls = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .in(FinishRoll::getOrderUuid, orderUuids));
        Map<String, List<FinishRoll>> grouped = new LinkedHashMap<>();
        for (FinishRoll roll : rolls) {
            grouped.computeIfAbsent(roll.getOrderUuid(), key -> new ArrayList<>()).add(roll);
        }
        return grouped;
    }

    private CandidateStats buildStats(List<OriginalRoll> originals, List<FinishRoll> finishes) {
        List<OriginalRoll> originalRows = originals == null ? List.of() : originals;
        List<FinishRoll> finishRows = finishes == null ? List.of() : finishes;
        return new CandidateStats(
                originalRows.size(),
                sumOriginalWeight(originalRows),
                (int) finishRows.stream().filter(this::isFormalFinishRoll).count(),
                sumFinishWeight(finishRows));
    }

    private BigDecimal sumOriginalWeight(List<OriginalRoll> rolls) {
        BigDecimal total = BigDecimal.ZERO;
        for (OriginalRoll roll : rolls) {
            BigDecimal weight = roll.getActualWeight() == null ? roll.getTotalWeight() : roll.getActualWeight();
            total = total.add(weight == null ? BigDecimal.ZERO : weight);
        }
        return total;
    }

    private BigDecimal sumFinishWeight(List<FinishRoll> rolls) {
        BigDecimal total = BigDecimal.ZERO;
        for (FinishRoll roll : rolls) {
            if (isFormalFinishRoll(roll)) {
                BigDecimal weight = roll.getActualWeight() == null ? roll.getEstimateWeight() : roll.getActualWeight();
                total = total.add(weight == null ? BigDecimal.ZERO : weight);
            }
        }
        return total;
    }

    private boolean isFormalFinishRoll(FinishRoll roll) {
        return roll.getIsSpare() == null || roll.getIsSpare() == IS_SPARE_NO;
    }

    public record CandidateStats(
            int originalRollCount,
            BigDecimal originalRollWeight,
            int finishRollCount,
            BigDecimal finishRollWeight) {
    }
}
