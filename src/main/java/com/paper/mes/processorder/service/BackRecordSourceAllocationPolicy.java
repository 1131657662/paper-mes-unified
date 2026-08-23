package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Rebuilds merged-source output shares after source rolls receive measured weights. */
public final class BackRecordSourceAllocationPolicy {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private BackRecordSourceAllocationPolicy() {
    }

    public static List<FinishOriginalRel> recalculate(List<OriginalRoll> rolls,
                                                       List<FinishRoll> finishes,
                                                       List<FinishOriginalRel> relations) {
        Map<String, OriginalRoll> rollByUuid = indexRolls(rolls);
        Map<String, FinishRoll> finishByUuid = indexFinishes(finishes);
        Map<String, List<FinishOriginalRel>> relationsByFinish = groupByFinish(relations);
        List<FinishOriginalRel> changed = new ArrayList<>();
        for (Map.Entry<String, List<FinishOriginalRel>> entry : relationsByFinish.entrySet()) {
            FinishRoll finish = finishByUuid.get(entry.getKey());
            if (finish == null) continue;
            recalculateFinish(entry.getValue(), finish, rollByUuid, changed);
        }
        return changed;
    }

    private static void recalculateFinish(List<FinishOriginalRel> relations, FinishRoll finish,
                                          Map<String, OriginalRoll> rollByUuid,
                                          List<FinishOriginalRel> changed) {
        List<BigDecimal> contributions = contributions(relations, rollByUuid);
        if (contributions.isEmpty()) return;
        BigDecimal total = contributions.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) return;

        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < relations.size(); index++) {
            BigDecimal ratio = index == relations.size() - 1
                    ? HUNDRED.subtract(allocated)
                    : contributions.get(index).multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
            FinishOriginalRel relation = relations.get(index);
            relation.setShareRatio(ratio.setScale(2, RoundingMode.HALF_UP));
            relation.setShareWeight(shareWeight(finish.getActualWeight(), relation.getShareRatio()));
            allocated = allocated.add(relation.getShareRatio());
            changed.add(relation);
        }
    }

    private static List<BigDecimal> contributions(List<FinishOriginalRel> relations,
                                                   Map<String, OriginalRoll> rollByUuid) {
        List<BigDecimal> result = new ArrayList<>(relations.size());
        Map<FinishOriginalRel, BigDecimal> effectiveRatios = effectiveRatios(relations);
        if (effectiveRatios.isEmpty() && !relations.isEmpty()) return List.of();
        for (FinishOriginalRel relation : relations) {
            OriginalRoll roll = rollByUuid.get(relation.getOriginalUuid());
            if (!positiveWeight(roll)) return List.of();
            result.add(roll.getActualWeight().multiply(effectiveRatios.get(relation))
                    .divide(HUNDRED, 6, RoundingMode.HALF_UP));
        }
        return result;
    }

    private static Map<FinishOriginalRel, BigDecimal> effectiveRatios(List<FinishOriginalRel> relations) {
        Map<FinishOriginalRel, BigDecimal> result = new java.util.IdentityHashMap<>();
        List<BigDecimal> ratios = SourceConsumptionRatioAllocator.allocate(relations.stream()
                .map(relation -> new SourceConsumptionRatioAllocator.SourceRatio(
                        relation.getOriginalUuid(), relation.getConsumeRatio())).toList());
        for (int index = 0; index < relations.size(); index++) {
            result.put(relations.get(index), ratios.get(index));
        }
        return result;
    }

    private static BigDecimal shareWeight(BigDecimal finishWeight, BigDecimal ratio) {
        if (!positive(finishWeight)) return null;
        return finishWeight.multiply(ratio).divide(HUNDRED, 3, RoundingMode.HALF_UP);
    }

    private static Map<String, OriginalRoll> indexRolls(List<OriginalRoll> rolls) {
        Map<String, OriginalRoll> result = new LinkedHashMap<>();
        for (OriginalRoll roll : rolls == null ? List.<OriginalRoll>of() : rolls) {
            result.put(roll.getUuid(), roll);
        }
        return result;
    }

    private static Map<String, FinishRoll> indexFinishes(List<FinishRoll> finishes) {
        Map<String, FinishRoll> result = new LinkedHashMap<>();
        for (FinishRoll finish : finishes == null ? List.<FinishRoll>of() : finishes) {
            result.put(finish.getUuid(), finish);
        }
        return result;
    }

    private static Map<String, List<FinishOriginalRel>> groupByFinish(List<FinishOriginalRel> relations) {
        Map<String, List<FinishOriginalRel>> result = new LinkedHashMap<>();
        for (FinishOriginalRel relation : relations == null ? List.<FinishOriginalRel>of() : relations) {
            result.computeIfAbsent(relation.getFinishUuid(), ignored -> new ArrayList<>()).add(relation);
        }
        return result;
    }

    private static boolean positiveWeight(OriginalRoll roll) {
        return roll != null && positive(roll.getActualWeight())
                && BackRecordRollMeasurementPolicy.isMeasured(roll);
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
