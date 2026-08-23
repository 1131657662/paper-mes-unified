package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.calc.IntegerWeightAllocator;
import com.paper.mes.processorder.model.WidthDifferencePolicy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SawWeightAllocator {

    private SawWeightAllocator() {
    }

    static List<BigDecimal> allocate(List<FinishConfigSpecDTO> specs,
                                     BigDecimal configuredWeight,
                                     BigDecimal differenceWeight,
                                     WidthDifferencePolicy policy,
                                     long sourceWidth) {
        BigDecimal outputBudget = policy == WidthDifferencePolicy.ALLOCATE
                ? configuredWeight.add(differenceWeight) : configuredWeight;
        BigDecimal trimBudget = trimBudget(specs, configuredWeight.add(differenceWeight), sourceWidth);
        BigDecimal finishBudget = outputBudget.subtract(trimBudget);
        List<Integer> finishes = indexes(specs, false);
        List<Integer> trims = indexes(specs, true);
        List<BigDecimal> weights = new ArrayList<>(Collections.nCopies(
                specs.size(), BigDecimal.ZERO.setScale(0)));
        place(weights, finishes, IntegerWeightAllocator.allocate(finishBudget,
                finishes.stream().map(index -> BigDecimal.valueOf(specs.get(index).getFinishWidth())).toList()));
        place(weights, trims, IntegerWeightAllocator.allocate(trimBudget, trimBases(specs, trims)));
        return weights;
    }

    private static BigDecimal trimBudget(List<FinishConfigSpecDTO> specs,
                                         BigDecimal sourceWeight, long sourceWidth) {
        long trimWidth = specs.stream().filter(SawWeightAllocator::isTrim)
                .mapToLong(spec -> spec.getFinishWidth()).sum();
        if (trimWidth == 0 || sourceWidth == 0) return BigDecimal.ZERO.setScale(0);
        return IntegerWeightAllocator.roundTotal(sourceWeight.multiply(BigDecimal.valueOf(trimWidth))
                .divide(BigDecimal.valueOf(sourceWidth), 12, java.math.RoundingMode.HALF_UP));
    }

    private static List<Integer> indexes(List<FinishConfigSpecDTO> specs, boolean trim) {
        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < specs.size(); index++) {
            if (isTrim(specs.get(index)) == trim) result.add(index);
        }
        return result;
    }

    private static List<BigDecimal> trimBases(List<FinishConfigSpecDTO> specs, List<Integer> trims) {
        return trims.stream().map(index -> BigDecimal.valueOf(specs.get(index).getFinishWidth())).toList();
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
