package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.model.WidthDifferencePolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SawWeightAllocator {

    private static final int SCALE = 3;
    private static final BigDecimal UNIT = new BigDecimal("0.001");

    private SawWeightAllocator() {
    }

    static List<BigDecimal> allocate(List<FinishConfigSpecDTO> specs,
                                     BigDecimal configuredWeight,
                                     BigDecimal differenceWeight,
                                     WidthDifferencePolicy policy,
                                     long usedWidth) {
        BigDecimal trimBudget = trimBudget(specs, configuredWeight, usedWidth);
        BigDecimal finishBudget = configuredWeight.subtract(trimBudget);
        if (policy == WidthDifferencePolicy.ALLOCATE) finishBudget = finishBudget.add(differenceWeight);
        List<Integer> finishes = indexes(specs, false);
        List<Integer> trims = indexes(specs, true);
        List<BigDecimal> weights = new ArrayList<>(Collections.nCopies(
                specs.size(), BigDecimal.ZERO.setScale(SCALE)));
        place(weights, finishes, equalShares(finishBudget, finishes.size()));
        place(weights, trims, proportionalTrimShares(specs, trims, trimBudget));
        return weights;
    }

    private static BigDecimal trimBudget(List<FinishConfigSpecDTO> specs,
                                         BigDecimal configuredWeight, long usedWidth) {
        long trimWidth = specs.stream().filter(SawWeightAllocator::isTrim)
                .mapToLong(spec -> spec.getFinishWidth()).sum();
        if (trimWidth == 0 || usedWidth == 0) return BigDecimal.ZERO.setScale(SCALE);
        return configuredWeight.multiply(BigDecimal.valueOf(trimWidth))
                .divide(BigDecimal.valueOf(usedWidth), SCALE, RoundingMode.HALF_UP);
    }

    private static List<Integer> indexes(List<FinishConfigSpecDTO> specs, boolean trim) {
        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < specs.size(); index++) {
            if (isTrim(specs.get(index)) == trim) result.add(index);
        }
        return result;
    }

    private static List<BigDecimal> equalShares(BigDecimal total, int count) {
        if (count == 0) return List.of();
        BigDecimal roundedTotal = total.setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal base = roundedTotal.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.DOWN);
        int extraUnits = roundedTotal.subtract(base.multiply(BigDecimal.valueOf(count)))
                .movePointRight(SCALE).intValueExact();
        List<BigDecimal> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(index < extraUnits ? base.add(UNIT) : base);
        }
        return result;
    }

    private static List<BigDecimal> proportionalTrimShares(
            List<FinishConfigSpecDTO> specs, List<Integer> trims, BigDecimal total) {
        if (trims.isEmpty()) return List.of();
        long width = trims.stream().mapToLong(index -> specs.get(index).getFinishWidth()).sum();
        List<BigDecimal> result = new ArrayList<>(trims.size());
        for (Integer index : trims) {
            result.add(total.multiply(BigDecimal.valueOf(specs.get(index).getFinishWidth()))
                    .divide(BigDecimal.valueOf(width), SCALE, RoundingMode.DOWN));
        }
        int units = total.setScale(SCALE, RoundingMode.HALF_UP)
                .subtract(result.stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                .movePointRight(SCALE).intValueExact();
        for (int index = 0; index < units; index++) {
            int target = index % result.size();
            result.set(target, result.get(target).add(UNIT));
        }
        return result;
    }

    private static void place(List<BigDecimal> target, List<Integer> indexes,
                              List<BigDecimal> values) {
        for (int index = 0; index < indexes.size(); index++) {
            target.set(indexes.get(index), values.get(index));
        }
    }

    private static boolean isTrim(FinishConfigSpecDTO spec) {
        return "TRIM".equalsIgnoreCase(spec.getItemType());
    }
}
