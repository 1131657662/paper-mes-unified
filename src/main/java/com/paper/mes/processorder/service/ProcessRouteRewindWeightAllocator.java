package com.paper.mes.processorder.service;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.calc.IntegerWeightAllocator;
import com.paper.mes.processorder.calc.RewindWeightCalculator;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import com.paper.mes.processorder.dto.RewindSourcePlanDTO;
import com.paper.mes.processorder.model.WidthDifferencePolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/** Applies rewind geometry, trim, loss, and allocation budgets to route outputs. */
final class ProcessRouteRewindWeightAllocator {
    private ProcessRouteRewindWeightAllocator() {
    }
    static List<BigDecimal> allocate(BigDecimal sourceWeight,
                                     ProcessRoutePreviewDTO.RouteStageDTO stage,
                                     List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs,
                                     Integer sourceWidth,
                                     Map<String, ProcessRoutePreviewVO.RouteOutputVO> inputs) {
        List<Output> expanded = expand(outputs);
        List<RewindSegmentPlanDTO> segments = stage.getPlan().getSegments();
        Map<RewindSourcePlanDTO, BigDecimal> effectiveRatios =
                ProcessRouteSegmentRatioResolver.effectiveRatios(segments);
        List<BigDecimal> ratios = ratios(stage, segments, inputs, effectiveRatios);
        List<BigDecimal> bases = finishBases(stage, segments, ratios, sourceWidth);
        WidthBudget budget = budget(sourceWeight, segments, ratios, sourceWidth,
                stage.getPlan().getRewindMode(),
                WidthDifferencePolicy.resolve(stage.getPlan().getWidthDifferencePolicy()));
        int finishCount = (int) expanded.stream().filter(output -> !output.trim()).count();
        List<BigDecimal> finishWeights = IntegerWeightAllocator.allocate(budget.baseWeight(),
                resizeBases(bases, finishCount));
        List<BigDecimal> trimBases = new ArrayList<>();
        for (int index = 0; index < expanded.size(); index++) {
            Output output = expanded.get(index);
            if (output.trim()) {
                trimBases.add(output.basis());
            }
        }
        List<BigDecimal> trimWeights = IntegerWeightAllocator.allocate(budget.trimWeight(), trimBases);
        // ALLOCATE assigns the width-difference budget to saleable finish rolls only.
        // Trim rolls already have their own physical-width budget and must not receive
        // an additional share of the difference.
        List<BigDecimal> allocationShares = IntegerWeightAllocator.allocate(budget.allocationWeight(),
                java.util.Collections.nCopies(finishCount, BigDecimal.ONE));
        List<BigDecimal> result = new ArrayList<>(expanded.size());
        int finishIndex = 0;
        int trimIndex = 0;
        for (int index = 0; index < expanded.size(); index++) {
            Output output = expanded.get(index);
            BigDecimal base = output.trim() ? trimWeights.get(trimIndex++) : finishWeights.get(finishIndex++);
            result.add(output.trim() ? base : base.add(allocationShares.get(finishIndex - 1)));
        }
        return result;
    }

    static BigDecimal expectedLossWeight(BigDecimal sourceWeight,
                                         ProcessRoutePreviewDTO.RouteStageDTO stage,
                                         int sourceWidth,
                                         Map<String, ProcessRoutePreviewVO.RouteOutputVO> inputs) {
        if (!isSegmentedRewind(stage) || sourceWidth <= 0) return null;
        List<RewindSegmentPlanDTO> segments = stage.getPlan().getSegments();
        List<BigDecimal> ratios = ratios(stage, segments, inputs,
                ProcessRouteSegmentRatioResolver.effectiveRatios(segments));
        BigDecimal difference = weightedDifference(segments, ratios, sourceWidth);
        return proportional(sourceWeight, difference, sourceWidth);
    }

    private static boolean isSegmentedRewind(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        return stage.getStepType() != null && stage.getStepType() == FeeCalculator.STEP_TYPE_REWIND
                && stage.getPlan() != null && stage.getPlan().getSegments() != null
                && !stage.getPlan().getSegments().isEmpty();
    }
    private static List<Output> expand(List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs) {
        List<Output> result = new ArrayList<>();
        for (var output : outputs) {
            for (int index = 0; index < positive(output.getCount()); index++) {
                boolean trim = output.getIsRemain() != null && output.getIsRemain() == 1;
                result.add(new Output(trim, BigDecimal.valueOf(Math.max(1, output.getFinishWidth() == null
                        ? 1 : output.getFinishWidth()))));
            }
        }
        return result;
    }
    private static List<BigDecimal> ratios(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                           List<RewindSegmentPlanDTO> segments,
                                           Map<String, ProcessRoutePreviewVO.RouteOutputVO> inputs,
                                           Map<RewindSourcePlanDTO, BigDecimal> effectiveRatios) {
        List<BigDecimal> raw = new ArrayList<>(segments.size());
        boolean modeFive = Integer.valueOf(5).equals(stage.getPlan().getRewindMode());
        for (var segment : segments) {
            BigDecimal consumed = consumedWeight(segment, inputs, effectiveRatios);
            if (modeFive && segment.getSources() != null && !segment.getSources().isEmpty()) {
                if (consumed.signum() <= 0) throw new BusinessException("合并复卷分段消耗重量必须大于0");
                raw.add(consumed);
            } else {
                raw.add(segment.getSegmentRatio() == null || segment.getSegmentRatio().signum() <= 0
                        ? BigDecimal.ONE : segment.getSegmentRatio());
            }
        }
        BigDecimal total = raw.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return raw.stream().map(value -> value.divide(total, 12, RoundingMode.HALF_UP)).toList();
    }
    private static BigDecimal consumedWeight(RewindSegmentPlanDTO segment,
                                             Map<String, ProcessRoutePreviewVO.RouteOutputVO> inputs,
                                             Map<RewindSourcePlanDTO, BigDecimal> effectiveRatios) {
        if (segment.getSources() == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (var source : segment.getSources()) {
            var input = inputs.get(source.getOriginalUuid());
            BigDecimal inputWeight = ProcessRoutePreviewValidator.effectiveOutputWeight(input);
            if (inputWeight != null) {
                total = total.add(inputWeight
                        .multiply(effectiveRatios.getOrDefault(source, BigDecimal.ZERO)).movePointLeft(2));
            }
        }
        return total;
    }
    private static List<BigDecimal> finishBases(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                                List<RewindSegmentPlanDTO> segments,
                                                List<BigDecimal> ratios,
                                                Integer sourceWidth) {
        List<BigDecimal> result = new ArrayList<>();
        for (int index = 0; index < segments.size(); index++) {
            RewindSegmentPlanDTO segment = segments.get(index);
            int repeat = positive(segment.getRepeatCount());
            BigDecimal repeatedRatio = ratios.get(index).divide(BigDecimal.valueOf(repeat), 12, RoundingMode.HALF_UP);
            for (int round = 0; round < repeat; round++) {
                for (var item : safe(segment.getLayoutItems())) {
                    if (isTrim(item)) continue;
                    for (int copy = 0; copy < positive(item.getQuantity()); copy++) {
                        result.add(rewindBasis(stage.getPlan().getRewindMode(), item, segment,
                                sourceWidth, repeatedRatio));
                    }
                }
            }
        }
        return result;
    }
    private static BigDecimal rewindBasis(Integer mode, RewindLayoutItemPlanDTO item,
                                          RewindSegmentPlanDTO segment, Integer sourceWidth,
                                          BigDecimal ratio) {
        BigDecimal width = BigDecimal.valueOf(Math.max(1, item.getWidth()));
        if (mode == null || mode == 1 || mode == 5 || mode == 6) return width.multiply(ratio);
        BigDecimal area = mode == 4 ? layeredArea(item) : crossSection(segment);
        if (mode == 2) return (area.signum() > 0 ? area : width).multiply(ratio);
        BigDecimal base = area.signum() > 0 ? area
                : BigDecimal.valueOf(sourceWidth == null || sourceWidth <= 0 ? item.getWidth() : sourceWidth);
        if (sourceWidth == null || sourceWidth <= 0) return width.multiply(ratio);
        return base.multiply(width).divide(BigDecimal.valueOf(sourceWidth), 12, RoundingMode.HALF_UP)
                .multiply(ratio);
    }
    private static WidthBudget budget(BigDecimal sourceWeight, List<RewindSegmentPlanDTO> segments,
                                      List<BigDecimal> ratios, Integer sourceWidth, Integer rewindMode,
                                      WidthDifferencePolicy policy) {
        if (sourceWidth == null || sourceWidth <= 0 || Integer.valueOf(2).equals(rewindMode)
                || Integer.valueOf(6).equals(rewindMode)) {
            return new WidthBudget(sourceWeight, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal explicit = BigDecimal.ZERO;
        BigDecimal difference = weightedDifference(segments, ratios, sourceWidth);
        for (int index = 0; index < segments.size(); index++) {
            var segment = segments.get(index);
            int finish = layoutWidth(segment, false);
            int trim = layoutWidth(segment, true);
            explicit = explicit.add(BigDecimal.valueOf(trim).multiply(ratios.get(index)));
        }
        BigDecimal outputTrimWidth = explicit.add(policy == WidthDifferencePolicy.REMAINDER ? difference : BigDecimal.ZERO);
        BigDecimal trimWeight = proportional(sourceWeight, outputTrimWidth, sourceWidth);
        BigDecimal differenceWeight = proportional(sourceWeight, difference, sourceWidth);
        BigDecimal loss = policy == WidthDifferencePolicy.LOSS ? differenceWeight : BigDecimal.ZERO;
        BigDecimal allocation = policy == WidthDifferencePolicy.ALLOCATE ? differenceWeight : BigDecimal.ZERO;
        return new WidthBudget(sourceWeight.subtract(trimWeight).subtract(loss).subtract(allocation),
                trimWeight, allocation, differenceWeight);
    }

    private static BigDecimal weightedDifference(List<RewindSegmentPlanDTO> segments,
                                                 List<BigDecimal> ratios,
                                                 int sourceWidth) {
        BigDecimal difference = BigDecimal.ZERO;
        for (int index = 0; index < segments.size(); index++) {
            int finish = layoutWidth(segments.get(index), false);
            int trim = layoutWidth(segments.get(index), true);
            difference = difference.add(BigDecimal.valueOf(Math.max(0, sourceWidth - finish - trim))
                    .multiply(ratios.get(index)));
        }
        return difference;
    }
    private static BigDecimal proportional(BigDecimal total, BigDecimal width, int sourceWidth) {
        return total.multiply(width).divide(BigDecimal.valueOf(sourceWidth), 12, RoundingMode.HALF_UP)
                .setScale(0, RoundingMode.HALF_UP);
    }
    private static List<BigDecimal> resizeBases(List<BigDecimal> bases, int count) {
        if (bases.size() >= count) return bases.subList(0, count);
        List<BigDecimal> result = new ArrayList<>(bases);
        while (result.size() < count) result.add(BigDecimal.ONE);
        return result;
    }
    private static int layoutWidth(RewindSegmentPlanDTO segment, boolean trim) {
        return safe(segment.getLayoutItems()).stream().filter(item -> isTrim(item) == trim)
                .mapToInt(item -> Math.max(1, item.getWidth()) * positive(item.getQuantity())).sum();
    }
    private static BigDecimal crossSection(RewindSegmentPlanDTO segment) {
        if (segment.getTargetDiameter() == null || segment.getFinishCoreDiameter() == null) return BigDecimal.ZERO;
        return RewindWeightCalculator.crossSectionArea(
                RewindWeightCalculator.storedDiameterToMm(BigDecimal.valueOf(segment.getTargetDiameter())),
                RewindWeightCalculator.storedCoreDiameterToMm(BigDecimal.valueOf(segment.getFinishCoreDiameter())));
    }
    private static BigDecimal layeredArea(RewindLayoutItemPlanDTO item) {
        if (item.getLayers() == null) return BigDecimal.ZERO;
        return item.getLayers().stream().filter(layer -> layer.getOutDiameter() != null && layer.getCoreDiameter() != null)
                .map(layer -> RewindWeightCalculator.crossSectionArea(
                        RewindWeightCalculator.storedDiameterToMm(BigDecimal.valueOf(layer.getOutDiameter())),
                        RewindWeightCalculator.storedCoreDiameterToMm(BigDecimal.valueOf(layer.getCoreDiameter()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private static boolean isTrim(RewindLayoutItemPlanDTO item) {
        return "TRIM".equalsIgnoreCase(item.getItemType());
    }
    private static int positive(Integer value) {
        return value == null ? 1 : Math.max(1, value);
    }
    private static List<RewindLayoutItemPlanDTO> safe(List<RewindLayoutItemPlanDTO> value) {
        return value == null ? List.of() : value;
    }
    private record Output(boolean trim, BigDecimal basis) {
    }
    private record WidthBudget(BigDecimal baseWeight, BigDecimal trimWeight,
                               BigDecimal allocationWeight, BigDecimal differenceWeight) {
    }
}
