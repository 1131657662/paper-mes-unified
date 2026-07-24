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

    public SawPlanCalculation calculate(List<FinishConfigSpecDTO> source, OriginalRoll roll, String policyValue) {
        List<FinishConfigSpecDTO> specs = source == null ? List.of() : source;
        List<FinishConfigSpecDTO> finishes = specs.stream().filter(this::isFinish).toList();
        WidthDifferencePolicy policy = resolvePolicy(policyValue, specs);
        int sourceWidth = effectiveSourceWidth(roll);
        int finishWidth = totalWidth(finishes);
        int differenceWidth = Math.max(0, sourceWidth - finishWidth);
        validate(specs, policy, sourceWidth, finishWidth, differenceWidth);
        BigDecimal differenceWeight = differenceWeight(roll, differenceWidth, sourceWidth);
        List<SawPlanCalculation.CalculatedFinish> rows = calculateFinishes(finishes, roll, policy, differenceWeight);
        int knives = rows.isEmpty() ? 0 : Math.max(0, rows.size() - 1) + (differenceWidth > 0 ? 1 : 0);
        return new SawPlanCalculation(rows, policy, sourceWidth, finishWidth,
                differenceWidth, differenceWeight, knives);
    }

    private List<SawPlanCalculation.CalculatedFinish> calculateFinishes(
            List<FinishConfigSpecDTO> specs, OriginalRoll roll,
            WidthDifferencePolicy policy, BigDecimal differenceWeight) {
        List<FinishConfigSpecDTO> expanded = expand(specs);
        BigDecimal allocatable = totalWeight(roll);
        if (policy != WidthDifferencePolicy.ALLOCATE) {
            allocatable = allocatable.subtract(differenceWeight);
        }
        BigDecimal widthBasis = BigDecimal.valueOf(totalWidth(specs));
        return allocate(expanded, allocatable, widthBasis);
    }

    private List<SawPlanCalculation.CalculatedFinish> allocate(
            List<FinishConfigSpecDTO> specs, BigDecimal weight, BigDecimal widthBasis) {
        List<SawPlanCalculation.CalculatedFinish> result = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < specs.size(); index++) {
            FinishConfigSpecDTO spec = specs.get(index);
            BigDecimal current = allocatedWeight(spec, index, specs.size(), weight, widthBasis, allocated);
            result.add(new SawPlanCalculation.CalculatedFinish(spec, current));
            allocated = allocated.add(current);
        }
        return result;
    }

    private BigDecimal allocatedWeight(FinishConfigSpecDTO spec, int index, int count,
                                       BigDecimal total, BigDecimal widthBasis, BigDecimal allocated) {
        if (count == 0 || total.signum() <= 0 || widthBasis.signum() <= 0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        if (index == count - 1) {
            return total.subtract(allocated).setScale(3, RoundingMode.HALF_UP);
        }
        BigDecimal width = BigDecimal.valueOf(spec.getFinishWidth() == null ? 0 : spec.getFinishWidth());
        return total.multiply(width).divide(widthBasis, 3, RoundingMode.HALF_UP);
    }

    private void validate(List<FinishConfigSpecDTO> specs, WidthDifferencePolicy policy,
                          int sourceWidth, int finishWidth, int differenceWidth) {
        if (finishWidth <= 0) {
            throw new BusinessException("锯纸至少需要一条成品规格");
        }
        if (sourceWidth > 0 && finishWidth > sourceWidth) {
            throw new BusinessException("锯纸成品门幅不能超过母卷门幅");
        }
        int explicitTrim = totalWidth(specs.stream().filter(this::isTrim).toList());
        if (sourceWidth > 0 && finishWidth + explicitTrim > sourceWidth) {
            throw new BusinessException("锯纸成品门幅加切边不能超过母卷门幅");
        }
        if (policy != WidthDifferencePolicy.REMAINDER && explicitTrim > 0) {
            throw new BusinessException("计损耗或分摊模式不能再添加实体切边");
        }
        if (policy == WidthDifferencePolicy.REMAINDER && explicitTrim > 0 && explicitTrim != differenceWidth) {
            throw new BusinessException("实体余料门幅必须等于母卷与成品的门幅差额");
        }
    }

    private List<FinishConfigSpecDTO> expand(List<FinishConfigSpecDTO> specs) {
        List<FinishConfigSpecDTO> result = new ArrayList<>();
        for (FinishConfigSpecDTO spec : specs) {
            for (int index = 0; index < count(spec); index++) {
                result.add(spec);
            }
        }
        return result;
    }

    private BigDecimal differenceWeight(OriginalRoll roll, int differenceWidth, int sourceWidth) {
        if (differenceWidth <= 0 || sourceWidth <= 0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        return totalWeight(roll).multiply(BigDecimal.valueOf(differenceWidth))
                .divide(BigDecimal.valueOf(sourceWidth), 3, RoundingMode.HALF_UP);
    }

    private BigDecimal totalWeight(OriginalRoll roll) {
        BigDecimal unit = roll.getRollWeight() == null ? BigDecimal.ZERO : roll.getRollWeight();
        return unit.multiply(BigDecimal.valueOf(roll.getPieceNum() == null ? 1 : roll.getPieceNum()));
    }

    private int effectiveSourceWidth(OriginalRoll roll) {
        if (roll.getActualWidth() != null && roll.getActualWidth() > 0) {
            return roll.getActualWidth();
        }
        return roll.getOriginalWidth() == null ? 0 : roll.getOriginalWidth();
    }

    private int totalWidth(List<FinishConfigSpecDTO> specs) {
        return specs.stream().mapToInt(spec -> (spec.getFinishWidth() == null ? 0 : spec.getFinishWidth()) * count(spec)).sum();
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

    private WidthDifferencePolicy resolvePolicy(String policyValue, List<FinishConfigSpecDTO> specs) {
        if (policyValue == null || policyValue.isBlank()) {
            boolean hasLegacyRemainder = specs.stream().anyMatch(this::isTrim);
            return hasLegacyRemainder ? WidthDifferencePolicy.REMAINDER : WidthDifferencePolicy.ALLOCATE;
        }
        return WidthDifferencePolicy.resolve(policyValue);
    }
}
