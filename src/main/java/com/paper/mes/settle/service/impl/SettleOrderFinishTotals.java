package com.paper.mes.settle.service.impl;

import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.settle.dto.SettlePrintLineVO;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SettleOrderFinishTotals {

    private static final int FLAG_YES = 1;
    private static final int ROLL_NO_VOID = 3;

    private SettleOrderFinishTotals() {
    }

    static void apply(List<SettlePrintLineVO> lines, List<FinishRoll> finishes) {
        Map<String, FinishRoll> unique = uniqueDeliverableFinishes(finishes);
        BigDecimal weight = unique.values().stream()
                .map(SettleOrderFinishTotals::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (SettlePrintLineVO line : lines) {
            line.setOrderFinishCount(unique.size());
            line.setOrderFinishWeight(weight);
        }
    }

    private static Map<String, FinishRoll> uniqueDeliverableFinishes(List<FinishRoll> finishes) {
        Map<String, FinishRoll> unique = new LinkedHashMap<>();
        for (FinishRoll finish : finishes) {
            if (!isDeliverable(finish)) continue;
            String key = finish.getUuid() != null ? finish.getUuid() : finish.getFinishRollNo();
            unique.put(key == null ? String.valueOf(unique.size()) : key, finish);
        }
        return unique;
    }

    private static boolean isDeliverable(FinishRoll finish) {
        return value(finish.getIsSpare()) != FLAG_YES
                && value(finish.getIsRemain()) != FLAG_YES
                && value(finish.getRollNoStatus()) != ROLL_NO_VOID;
    }

    private static BigDecimal weight(FinishRoll finish) {
        BigDecimal value = finish.getActualWeight() != null
                ? finish.getActualWeight()
                : finish.getEstimateWeight();
        return value == null ? BigDecimal.ZERO : value;
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }
}
