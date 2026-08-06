package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.model.WidthDifferencePolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class SawPlanCalculator {

    private static final String ITEM_TRIM = "TRIM";
    private static final int SCALE = 3;

    public SawPlanCalculation calculate(List<FinishConfigSpecDTO> source, OriginalRoll roll,
                                        String policyValue) {
        List<FinishConfigSpecDTO> specs = source == null ? List.of() : source;
        WidthDifferencePolicy policy = WidthDifferencePolicy.resolve(policyValue);
        int sourceWidth = effectiveSourceWidth(roll);
        int finishWidth = totalWidth(specs.stream().filter(this::isFinish).toList());
        int trimWidth = totalWidth(specs.stream().filter(this::isTrim).toList());
        int usedWidth = finishWidth + trimWidth;
        int differenceWidth = Math.max(0, sourceWidth - usedWidth);
        validate(policy, sourceWidth, finishWidth, usedWidth, differenceWidth);

        int sourcePieces = sourcePieceCount(roll);
        BigDecimal totalWeight = totalWeight(roll, sourcePieces);
        BigDecimal differenceWeight = proportionalWeight(totalWeight, differenceWidth, sourceWidth);
        List<FinishConfigSpecDTO> perPieceOutputs = expand(specs);
        List<FinishConfigSpecDTO> expanded = repeatForSourcePieces(perPieceOutputs, sourcePieces);
        List<SawPlanCalculation.CalculatedFinish> calculated = calculateOutputs(
                expanded, totalWeight, differenceWeight, policy, (long) usedWidth * sourcePieces);
        List<SawPlanCalculation.CalculatedFinish> finishes = calculated.stream()
                .filter(item -> isFinish(item.specification())).toList();
        List<SawPlanCalculation.CalculatedFinish> trims = calculated.stream()
                .filter(item -> isTrim(item.specification())).toList();
        int physicalPieces = perPieceOutputs.size() + (differenceWidth > 0 ? 1 : 0);
        int knivesPerSource = physicalPieces <= 0 ? 0 : Math.max(0, physicalPieces - 1);
        int knives = knivesPerSource * sourcePieces;
        return new SawPlanCalculation(finishes, trims, policy, sourceWidth, finishWidth,
                trimWidth, differenceWidth, differenceWeight, knives);
    }

    private List<SawPlanCalculation.CalculatedFinish> calculateOutputs(
            List<FinishConfigSpecDTO> specs, BigDecimal totalWeight,
            BigDecimal differenceWeight, WidthDifferencePolicy policy, long usedWidth) {
        BigDecimal configuredWeight = totalWeight.subtract(differenceWeight);
        List<BigDecimal> weights = allocateByWidth(specs, configuredWeight, usedWidth);
        if (policy == WidthDifferencePolicy.ALLOCATE && differenceWeight.signum() > 0) {
            distributeEqually(specs, weights, differenceWeight);
        }
        List<SawPlanCalculation.CalculatedFinish> result = new ArrayList<>(specs.size());
        for (int index = 0; index < specs.size(); index++) {
            result.add(new SawPlanCalculation.CalculatedFinish(specs.get(index), weights.get(index)));
        }
        return result;
    }

    private List<BigDecimal> allocateByWidth(List<FinishConfigSpecDTO> specs,
                                             BigDecimal total, long widthBasis) {
        List<BigDecimal> result = new ArrayList<>(specs.size());
        for (FinishConfigSpecDTO spec : specs) {
            result.add(total.multiply(BigDecimal.valueOf(spec.getFinishWidth()))
                    .divide(BigDecimal.valueOf(widthBasis), SCALE, RoundingMode.HALF_UP));
        }
        applyRoundingRemainder(specs, result, total);
        return result;
    }

    private void distributeEqually(List<FinishConfigSpecDTO> specs, List<BigDecimal> weights,
                                   BigDecimal differenceWeight) {
        if (weights.isEmpty()) return;
        BigDecimal share = differenceWeight.divide(BigDecimal.valueOf(weights.size()),
                SCALE, RoundingMode.HALF_UP);
        BigDecimal expectedTotal = sum(weights).add(differenceWeight);
        for (int index = 0; index < weights.size(); index++) {
            weights.set(index, weights.get(index).add(share).setScale(SCALE, RoundingMode.HALF_UP));
        }
        applyRoundingRemainder(specs, weights, expectedTotal);
    }

    private void applyRoundingRemainder(List<FinishConfigSpecDTO> specs,
                                        List<BigDecimal> weights, BigDecimal expectedTotal) {
        BigDecimal remainder = expectedTotal.subtract(sum(weights)).setScale(SCALE, RoundingMode.HALF_UP);
        if (remainder.signum() == 0) return;
        int target = lastFinishIndex(specs);
        weights.set(target, weights.get(target).add(remainder).setScale(SCALE, RoundingMode.HALF_UP));
    }

    private BigDecimal sum(List<BigDecimal> weights) {
        return weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int lastFinishIndex(List<FinishConfigSpecDTO> specs) {
        for (int index = specs.size() - 1; index >= 0; index--) {
            if (isFinish(specs.get(index))) return index;
        }
        throw new BusinessException("锯纸至少需要一条成品规格");
    }

    private void validate(WidthDifferencePolicy policy, int sourceWidth, int finishWidth,
                          int usedWidth, int differenceWidth) {
        if (finishWidth <= 0) {
            throw new BusinessException("锯纸至少需要一条成品规格");
        }
        if (sourceWidth <= 0) {
            throw new BusinessException("母卷有效门幅必须大于0");
        }
        if (usedWidth > sourceWidth) {
            throw new BusinessException("锯纸成品门幅加切边不能超过母卷门幅");
        }
        if (policy == WidthDifferencePolicy.REMAINDER && differenceWidth > 0) {
            throw new BusinessException("留余料模式还有 " + differenceWidth
                    + "mm 未分配，请补齐余料后再保存");
        }
    }

    private List<FinishConfigSpecDTO> expand(List<FinishConfigSpecDTO> specs) {
        List<FinishConfigSpecDTO> result = new ArrayList<>();
        for (FinishConfigSpecDTO spec : specs) {
            for (int index = 0; index < count(spec); index++) result.add(spec);
        }
        return result;
    }

    private List<FinishConfigSpecDTO> repeatForSourcePieces(List<FinishConfigSpecDTO> outputs,
                                                             int sourcePieces) {
        long total = (long) outputs.size() * sourcePieces;
        if (total > FinishConfigQuantityValidator.MAX_TOTAL_FINISHES) {
            throw new BusinessException("单个母卷展开后的成品和余料总数不能超过500");
        }
        List<FinishConfigSpecDTO> result = new ArrayList<>((int) total);
        for (int index = 0; index < sourcePieces; index++) result.addAll(outputs);
        return result;
    }

    private BigDecimal proportionalWeight(BigDecimal totalWeight, int width, int sourceWidth) {
        if (width <= 0 || sourceWidth <= 0) return BigDecimal.ZERO.setScale(SCALE);
        return totalWeight.multiply(BigDecimal.valueOf(width))
                .divide(BigDecimal.valueOf(sourceWidth), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal totalWeight(OriginalRoll roll, int sourcePieces) {
        BigDecimal unit = roll.getRollWeight() == null ? BigDecimal.ZERO : roll.getRollWeight();
        return unit.multiply(BigDecimal.valueOf(sourcePieces));
    }

    private int sourcePieceCount(OriginalRoll roll) {
        int count = roll.getPieceNum() == null ? 1 : roll.getPieceNum();
        if (count < 1) throw new BusinessException("母卷件数必须大于0");
        return count;
    }

    private int effectiveSourceWidth(OriginalRoll roll) {
        if (roll.getActualWidth() != null && roll.getActualWidth() > 0) return roll.getActualWidth();
        return roll.getOriginalWidth() == null ? 0 : roll.getOriginalWidth();
    }

    private int totalWidth(List<FinishConfigSpecDTO> specs) {
        return specs.stream().mapToInt(spec ->
                (spec.getFinishWidth() == null ? 0 : spec.getFinishWidth()) * count(spec)).sum();
    }

    private int count(FinishConfigSpecDTO spec) {
        return spec.getCount() == null ? 1 : spec.getCount();
    }

    private boolean isFinish(FinishConfigSpecDTO spec) {
        return !isTrim(spec);
    }

    private boolean isTrim(FinishConfigSpecDTO spec) {
        return ITEM_TRIM.equalsIgnoreCase(spec.getItemType());
    }
}
