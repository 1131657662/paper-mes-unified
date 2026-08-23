package com.paper.mes.processorder.service;

import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.calc.IntegerWeightAllocator;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.model.WidthDifferencePolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Generates route output estimates from physical route inputs, never from client weights. */
final class ProcessRouteCanonicalWeightCalculator {

    private ProcessRouteCanonicalWeightCalculator() {
    }

    static List<BigDecimal> allocate(BigDecimal sourceWeight,
                                     ProcessRoutePreviewDTO.RouteStageDTO stage,
                                     List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs,
                                     ProcessRouteWidthValidator.WidthBalance width,
                                     Map<String, ProcessRoutePreviewVO.RouteOutputVO> inputs) {
        if (isSegmentedRewind(stage)) {
            return ProcessRouteRewindWeightAllocator.allocate(sourceWeight, stage, outputs,
                    width.sourceWidth(), inputs);
        }
        List<ProcessRoutePreviewDTO.RouteOutputDTO> expanded = expand(outputs);
        if (expanded.isEmpty()) return List.of();
        WidthDifferencePolicy differencePolicy = policy(stage);
        BigDecimal target = targetWeight(sourceWeight, width, differencePolicy);
        if (width.sourceWidth() == null || width.sourceWidth() <= 0) {
            return IntegerWeightAllocator.allocate(target, expanded.stream()
                    .map(ProcessRouteCanonicalWeightCalculator::basis).toList());
        }
        List<Integer> finishIndexes = indexes(expanded, false);
        List<Integer> trimIndexes = indexes(expanded, true);
        BigDecimal trimBudget = explicitTrimBudget(sourceWeight, width, expanded, differencePolicy);
        BigDecimal finishBudget = target.subtract(trimBudget);
        if (finishBudget.signum() < 0) {
            throw new com.paper.mes.common.BusinessException("显式余料重量不能超过阶段可分配重量");
        }
        List<BigDecimal> result = new ArrayList<>(java.util.Collections.nCopies(
                expanded.size(), BigDecimal.ZERO.setScale(0)));
        place(result, finishIndexes, IntegerWeightAllocator.allocate(finishBudget,
                finishIndexes.stream().map(index -> basis(expanded.get(index))).toList()));
        place(result, trimIndexes, IntegerWeightAllocator.allocate(trimBudget,
                trimIndexes.stream().map(index -> basis(expanded.get(index))).toList()));
        return result;
    }

    private static boolean isSegmentedRewind(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        return stage.getStepType() != null && stage.getStepType() == FeeCalculator.STEP_TYPE_REWIND
                && stage.getPlan() != null && stage.getPlan().getSegments() != null
                && !stage.getPlan().getSegments().isEmpty();
    }

    private static List<ProcessRoutePreviewDTO.RouteOutputDTO> expand(
            List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs) {
        List<ProcessRoutePreviewDTO.RouteOutputDTO> result = new ArrayList<>();
        for (var output : outputs) {
            for (int index = 0; index < positive(output.getCount()); index++) result.add(output);
        }
        return result;
    }

    private static BigDecimal targetWeight(BigDecimal sourceWeight,
                                           ProcessRouteWidthValidator.WidthBalance width,
                                           WidthDifferencePolicy policy) {
        if (policy != WidthDifferencePolicy.LOSS || width.sourceWidth() == null
                || width.outputWidth() == null || width.outputWidth() >= width.sourceWidth()) {
            return sourceWeight;
        }
        BigDecimal loss = IntegerWeightAllocator.roundTotal(sourceWeight
                .multiply(BigDecimal.valueOf(width.sourceWidth() - width.outputWidth()))
                .divide(BigDecimal.valueOf(width.sourceWidth()), 12, RoundingMode.HALF_UP));
        return sourceWeight.subtract(loss);
    }

    private static WidthDifferencePolicy policy(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        if (stage.getPlan() == null) return null;
        if (stage.getStepType() == FeeCalculator.STEP_TYPE_SAW
                || (stage.getStepType() == FeeCalculator.STEP_TYPE_REWIND
                && RewindWidthDifferenceCalculator.supportsWidthPolicy(stage.getPlan().getRewindMode()))) {
            return WidthDifferencePolicy.resolve(stage.getPlan().getWidthDifferencePolicy());
        }
        return null;
    }

    private static int positive(Integer value) {
        return value == null ? 1 : Math.max(1, value);
    }

    private static List<Integer> indexes(List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs, boolean trim) {
        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < outputs.size(); index++) {
            if (isTrim(outputs.get(index)) == trim) result.add(index);
        }
        return result;
    }

    private static boolean isTrim(ProcessRoutePreviewDTO.RouteOutputDTO output) {
        return output.getIsRemain() != null && output.getIsRemain() == 1;
    }

    private static BigDecimal basis(ProcessRoutePreviewDTO.RouteOutputDTO output) {
        return BigDecimal.valueOf(output.getFinishWidth() == null
                ? 1 : Math.max(1, output.getFinishWidth()));
    }

    private static BigDecimal explicitTrimBudget(BigDecimal sourceWeight,
                                                  ProcessRouteWidthValidator.WidthBalance width,
                                                  List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs,
                                                  WidthDifferencePolicy differencePolicy) {
        if (sourceWeight == null || width.sourceWidth() == null || width.sourceWidth() <= 0) {
            return BigDecimal.ZERO.setScale(0);
        }
        long trimWidth = outputs.stream().filter(ProcessRouteCanonicalWeightCalculator::isTrim)
                .mapToLong(output -> output.getFinishWidth() == null ? 0 : Math.max(0, output.getFinishWidth()))
                .sum();
        if (differencePolicy == WidthDifferencePolicy.REMAINDER && width.outputWidth() != null) {
            trimWidth = Math.max(trimWidth, width.sourceWidth() - width.outputWidth() + trimWidth);
        }
        if (trimWidth <= 0) return BigDecimal.ZERO.setScale(0);
        return IntegerWeightAllocator.roundTotal(sourceWeight
                .multiply(BigDecimal.valueOf(trimWidth))
                .divide(BigDecimal.valueOf(width.sourceWidth()), 12, RoundingMode.HALF_UP));
    }

    private static void place(List<BigDecimal> target, List<Integer> indexes, List<BigDecimal> values) {
        for (int index = 0; index < indexes.size(); index++) {
            target.set(indexes.get(index), values.get(index));
        }
    }
}
