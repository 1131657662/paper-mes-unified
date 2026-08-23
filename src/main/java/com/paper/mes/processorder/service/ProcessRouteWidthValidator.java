package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.dto.RewindSourcePlanDTO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.model.WidthDifferencePolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ProcessRouteWidthValidator {

    private ProcessRouteWidthValidator() {
    }

    static WidthBalance validate(OriginalRoll roll,
                                 ProcessRoutePreviewDTO.RouteStageDTO stage,
                                 Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey,
                                 List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs) {
        validateRewindSources(stage, outputsByKey);
        Integer sourceWidth = sourceWidth(roll, stage, outputsByKey);
        WidthDifferencePolicy policy = widthPolicy(stage);
        ProcessRouteOutputShapeValidator.validate(stage, outputs, sourceWidth, policy, outputsByKey);
        Integer outputWidth = outputWidth(stage, outputs, sourceWidth, policy, outputsByKey);
        if (sourceWidth != null && outputWidth == null) {
            throw new BusinessException("阶段产物门幅未知，不能校验来源门幅闭合");
        }
        if (sourceWidth != null && outputWidth > sourceWidth) {
            throw new BusinessException("阶段产出总门幅不能超过来源门幅");
        }
        if (policy == WidthDifferencePolicy.REMAINDER
                && sourceWidth != null && outputWidth != null && !sourceWidth.equals(outputWidth)) {
            throw new BusinessException("留余料模式必须把门幅差额计入余料");
        }
        if (policy == WidthDifferencePolicy.REMAINDER && isSegmentedRewind(stage)
                && sourceWidth != null && rewindDifferenceWidth(stage, sourceWidth, outputsByKey,
                ProcessRouteSegmentRatioResolver.effectiveRatios(stage.getPlan().getSegments())) > 0
                && outputs.stream().noneMatch(output -> output.getIsRemain() != null && output.getIsRemain() == 1)) {
            throw new BusinessException("留余料模式必须生成余料产物");
        }
        return new WidthBalance(sourceWidth, outputWidth);
    }

    private static Integer sourceWidth(OriginalRoll roll,
                                       ProcessRoutePreviewDTO.RouteStageDTO stage,
                                       Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey) {
        if (stage.getInputOutputKeys() == null || stage.getInputOutputKeys().isEmpty()) {
            Integer width = roll.getActualWidth() != null && roll.getActualWidth() > 0
                    ? roll.getActualWidth() : roll.getOriginalWidth();
            if (width == null || width <= 0) return null;
            return width;
        }
        Integer expected = null;
        for (String key : stage.getInputOutputKeys()) {
            ProcessRoutePreviewVO.RouteOutputVO output = outputsByKey.get(key);
            if (output == null || output.getFinishWidth() == null || output.getFinishWidth() <= 0) return null;
            if (expected == null) {
                expected = output.getFinishWidth();
            } else if (!expected.equals(output.getFinishWidth())) {
                throw new BusinessException("链式工艺来源产物门幅必须一致");
            }
        }
        return expected;
    }

    private static void validateRewindSources(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                              Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey) {
        if (stage.getStepType() != FeeCalculator.STEP_TYPE_REWIND
                || stage.getPlan() == null || !Integer.valueOf(5).equals(stage.getPlan().getRewindMode())) {
            return;
        }
        List<String> inputKeys = stage.getInputOutputKeys() == null
                ? List.of() : stage.getInputOutputKeys();
        if (inputKeys.isEmpty()) {
            return;
        }
        if (stage.getPlan().getSegments() == null || stage.getPlan().getSegments().isEmpty()) {
            throw new BusinessException("合并复卷至少需要一个来源分段");
        }
        Set<String> configured = new HashSet<>();
        Map<String, BigDecimal> consumedBySource = new LinkedHashMap<>();
        Map<RewindSourcePlanDTO, BigDecimal> effectiveRatios = ProcessRouteSegmentRatioResolver.effectiveRatios(
                stage.getPlan().getSegments());
        for (var segment : stage.getPlan().getSegments()) {
            if (segment.getSources() == null || segment.getSources().isEmpty()) {
                throw new BusinessException("合并复卷每个分段必须选择来源产物");
            }
            for (var source : segment.getSources()) {
                String key = source.getOriginalUuid();
                if (key == null || key.isBlank() || !inputKeys.contains(key)
                        || !outputsByKey.containsKey(key)) {
                    throw new BusinessException("合并复卷来源产物必须来自本阶段输入");
                }
                BigDecimal consumeRatio = effectiveRatios.getOrDefault(source, BigDecimal.ZERO);
                if (source.getShareRatio() != null && (source.getShareRatio().signum() < 0
                        || source.getShareRatio().compareTo(new BigDecimal("100")) > 0)) {
                    throw new BusinessException("来源分摊比例必须在0到100%之间");
                }
                consumedBySource.merge(key, consumeRatio, BigDecimal::add);
                configured.add(key);
            }
            if (segmentConsumedWeight(segment, outputsByKey, effectiveRatios).signum() <= 0
                    && hasFinishLayout(segment)) {
                throw new BusinessException("合并复卷分段消耗重量必须大于0");
            }
        }
        if (!configured.containsAll(inputKeys)) {
            throw new BusinessException("合并复卷必须覆盖本阶段选择的全部来源产物");
        }
        for (String key : inputKeys) {
            BigDecimal total = consumedBySource.getOrDefault(key, BigDecimal.ZERO).min(new BigDecimal("100"));
            if (total.compareTo(new BigDecimal("100")) != 0) {
                throw new BusinessException("来源 " + key + " 的消耗比例合计必须为100%");
            }
        }
    }

    private static BigDecimal segmentConsumedWeight(RewindSegmentPlanDTO segment,
                                                    Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey,
                                                    Map<RewindSourcePlanDTO, BigDecimal> effectiveRatios) {
        BigDecimal total = BigDecimal.ZERO;
        for (RewindSourcePlanDTO source : segment.getSources() == null
                ? List.<RewindSourcePlanDTO>of() : segment.getSources()) {
            var input = outputsByKey.get(source.getOriginalUuid());
            BigDecimal inputWeight = ProcessRoutePreviewValidator.effectiveOutputWeight(input);
            if (inputWeight != null) {
                total = total.add(inputWeight
                        .multiply(effectiveRatios.getOrDefault(source, BigDecimal.ZERO))
                        .movePointLeft(2));
            }
        }
        return total;
    }

    private static boolean hasFinishLayout(RewindSegmentPlanDTO segment) {
        return segment.getLayoutItems() != null && segment.getLayoutItems().stream()
                .anyMatch(item -> !"TRIM".equalsIgnoreCase(item.getItemType())
                        && (item.getQuantity() == null || item.getQuantity() > 0));
    }

    private static Integer outputWidth(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                       List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs,
                                       Integer sourceWidth,
                                       WidthDifferencePolicy policy,
                                       Map<String, ProcessRoutePreviewVO.RouteOutputVO> inputs) {
        if (isSegmentedRewind(stage) && sourceWidth != null) {
            validateRewindLayout(stage, sourceWidth);
            if (policy == WidthDifferencePolicy.REMAINDER) return sourceWidth;
            Map<RewindSourcePlanDTO, BigDecimal> effectiveRatios =
                    ProcessRouteSegmentRatioResolver.effectiveRatios(stage.getPlan().getSegments());
            return Math.max(0, sourceWidth - rewindDifferenceWidth(stage, sourceWidth, inputs,
                    effectiveRatios));
        }
        int total = 0;
        for (ProcessRoutePreviewDTO.RouteOutputDTO output : outputs) {
            if (output.getFinishWidth() == null || output.getFinishWidth() <= 0) return null;
            total += output.getFinishWidth() * (output.getCount() == null ? 1 : output.getCount());
        }
        return total > 0 ? total : null;
    }

    private static WidthDifferencePolicy widthPolicy(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        if (stage.getPlan() == null) return WidthDifferencePolicy.REMAINDER;
        if (stage.getStepType() == FeeCalculator.STEP_TYPE_SAW) {
            return WidthDifferencePolicy.resolve(stage.getPlan().getWidthDifferencePolicy());
        }
        if (stage.getStepType() == FeeCalculator.STEP_TYPE_REWIND
                && RewindWidthDifferenceCalculator.supportsWidthPolicy(stage.getPlan().getRewindMode())) {
            return WidthDifferencePolicy.resolve(stage.getPlan().getWidthDifferencePolicy());
        }
        return null;
    }

    private static boolean isSegmentedRewind(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        return stage.getStepType() == FeeCalculator.STEP_TYPE_REWIND
                && stage.getPlan() != null && stage.getPlan().getSegments() != null
                && !stage.getPlan().getSegments().isEmpty();
    }

    private static void validateRewindLayout(ProcessRoutePreviewDTO.RouteStageDTO stage, int sourceWidth) {
        for (var segment : stage.getPlan().getSegments()) {
            if (layoutWidth(segment.getLayoutItems()) > sourceWidth) {
                throw new BusinessException("复卷阶段产出总门幅不能超过来源门幅");
            }
        }
    }

    private static int rewindDifferenceWidth(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                             int sourceWidth,
                                             Map<String, ProcessRoutePreviewVO.RouteOutputVO> inputs,
                                             Map<RewindSourcePlanDTO, BigDecimal> effectiveRatios) {
        BigDecimal weighted = BigDecimal.ZERO;
        for (var segment : stage.getPlan().getSegments()) {
            BigDecimal ratio = ProcessRouteSegmentRatioResolver.segmentRatio(
                    stage, segment, inputs, effectiveRatios);
            weighted = weighted.add(BigDecimal.valueOf(Math.max(0, sourceWidth - layoutWidth(segment.getLayoutItems())))
                    .multiply(ratio));
        }
        return weighted.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private static int layoutWidth(List<RewindLayoutItemPlanDTO> items) {
        if (items == null) return 0;
        return items.stream()
                .mapToInt(item -> item.getWidth() * (item.getQuantity() == null ? 1 : item.getQuantity()))
                .sum();
    }

    record WidthBalance(Integer sourceWidth, Integer outputWidth) {
    }
}
