package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.calc.IntegerWeightAllocator;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.model.WeightStatus;
import com.paper.mes.processorder.model.WidthDifferencePolicy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

final class ProcessRoutePreviewValidator {

    private ProcessRoutePreviewValidator() {
    }

    static StageBalance validateStageWeight(OriginalRoll roll,
                                            ProcessRoutePreviewDTO.RouteStageDTO stage,
                                            Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey) {
        List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs = stage.getOutputs() == null
                ? List.of() : stage.getOutputs();
        ProcessRouteWidthValidator.WidthBalance width =
                ProcessRouteWidthValidator.validate(roll, stage, outputsByKey, outputs);
        List<BigDecimal> weights = ProcessRouteCanonicalWeightCalculator.allocate(
                stageSourceWeight(roll, stage, outputsByKey), stage, outputs, width, outputsByKey);
        return validateStageWeight(roll, stage, outputsByKey, width, weights);
    }

    static StageBalance validateStageWeight(OriginalRoll roll,
                                            ProcessRoutePreviewDTO.RouteStageDTO stage,
                                            Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey,
                                            ProcessRouteWidthValidator.WidthBalance width,
                                            List<BigDecimal> authoritativeWeights) {
        List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs = stage.getOutputs() == null
                ? List.of()
                : stage.getOutputs();
        if (outputs.isEmpty()) {
            throw new BusinessException("每道工序至少需要一个阶段产物");
        }
        BigDecimal sourceWeight = stageSourceWeight(roll, stage, outputsByKey);
        if (sourceWeight == null) {
            throw new BusinessException("来源母卷重量未知，不能生成闭合的阶段预估");
        }
        Integer sourceWidth = width.sourceWidth();
        Integer outputWidth = width.outputWidth();
        BigDecimal outputWeight = outputWeight(authoritativeWeights);
        WidthDifferencePolicy policy = widthPolicy(stage);
        validateWeights(sourceWeight, outputWeight, policy, sourceWidth, outputWidth, stage, outputsByKey);
        return new StageBalance(sourceWeight, outputWeight, sourceWidth, outputWidth, policy);
    }

    static BigDecimal stageSourceWeight(OriginalRoll roll,
                                        ProcessRoutePreviewDTO.RouteStageDTO stage,
                                        Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey) {
        if (stage.getInputOutputKeys() == null || stage.getInputOutputKeys().isEmpty()) {
            return originalWeight(roll);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (String key : stage.getInputOutputKeys()) {
            ProcessRoutePreviewVO.RouteOutputVO output = outputsByKey.get(key);
            if (output == null) throw new BusinessException("阶段输入产物不存在：" + key);
            BigDecimal weight = outputEstimateWeight(output);
            if (weight == null) throw new BusinessException("阶段输入产物重量未知：" + key);
            total = total.add(weight);
        }
        return IntegerWeightAllocator.roundTotal(total);
    }

    static BigDecimal originalWeight(OriginalRoll roll) {
        BigDecimal weight;
        if (roll.getActualWeight() != null && roll.getActualWeight().signum() > 0) {
            weight = roll.getActualWeight();
        } else if (WeightStatus.UNKNOWN.name().equalsIgnoreCase(roll.getWeightStatus())) {
            return null;
        } else if (roll.getTotalWeight() != null && roll.getTotalWeight().signum() > 0) {
            weight = roll.getTotalWeight();
        } else {
            BigDecimal unit = roll.getRollWeight() == null ? BigDecimal.ZERO : roll.getRollWeight();
            BigDecimal pieces = BigDecimal.valueOf(roll.getPieceNum() == null ? 1 : roll.getPieceNum());
            weight = unit.multiply(pieces);
        }
        return weight.signum() > 0 ? IntegerWeightAllocator.roundTotal(weight) : null;
    }

    private static BigDecimal outputWeight(List<BigDecimal> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new BusinessException("每道工序至少需要一个阶段产物");
        }
        return weights.stream().map(value -> {
            if (value == null || value.signum() <= 0) {
                throw new BusinessException("阶段产物预估重量分配后必须大于0");
            }
            return IntegerWeightAllocator.roundTotal(value);
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal outputEstimateWeight(ProcessRoutePreviewVO.RouteOutputVO output) {
        BigDecimal weight = effectiveOutputWeight(output);
        return weight == null || weight.signum() <= 0 ? null : weight;
    }

    static BigDecimal effectiveOutputWeight(ProcessRoutePreviewVO.RouteOutputVO output) {
        if (output == null) return null;
        if (output.getActualWeight() != null && output.getActualWeight().signum() > 0) {
            return output.getActualWeight();
        }
        return output.getEstimateWeight();
    }

    private static void validateWeights(BigDecimal sourceWeight, BigDecimal outputWeight,
                                        WidthDifferencePolicy policy,
                                        Integer sourceWidth, Integer outputWidth,
                                        ProcessRoutePreviewDTO.RouteStageDTO stage,
                                        Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey) {
        if (outputWeight.compareTo(sourceWeight) > 0) {
            throw new BusinessException("阶段产物预估重量不能超过来源重量");
        }
        if (policy != WidthDifferencePolicy.LOSS) {
            if (outputWeight.compareTo(sourceWeight) != 0) {
                throw new BusinessException("阶段产物预估重量必须与来源重量闭合（成品+余料=来源）");
            }
            return;
        }
        if (sourceWidth == null || outputWidth == null) {
            throw new BusinessException("计损耗模式必须提供来源和产出门幅");
        }
        BigDecimal expectedLoss = ProcessRouteRewindWeightAllocator.expectedLossWeight(
                sourceWeight, stage, sourceWidth, outputsByKey);
        if (expectedLoss == null) {
            expectedLoss = IntegerWeightAllocator.roundTotal(sourceWeight
                    .multiply(BigDecimal.valueOf(sourceWidth - outputWidth))
                    .divide(BigDecimal.valueOf(sourceWidth), 12, java.math.RoundingMode.HALF_UP));
        }
        if (sourceWeight.subtract(outputWeight).compareTo(expectedLoss) != 0) {
            throw new BusinessException("阶段计划损耗重量与门幅差额不一致");
        }
    }

    private static WidthDifferencePolicy widthPolicy(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        if (stage.getPlan() == null) return null;
        if (stage.getStepType() == FeeCalculator.STEP_TYPE_SAW) {
            return WidthDifferencePolicy.resolve(stage.getPlan().getWidthDifferencePolicy());
        }
        if (stage.getStepType() == FeeCalculator.STEP_TYPE_REWIND
                && supportsWidthPolicy(stage.getPlan().getRewindMode())) {
            return WidthDifferencePolicy.resolve(stage.getPlan().getWidthDifferencePolicy());
        }
        return null;
    }

    private static boolean supportsWidthPolicy(Integer rewindMode) {
        return rewindMode != null && (rewindMode == 1 || rewindMode == 3
                || rewindMode == 4 || rewindMode == 5);
    }

    record StageBalance(BigDecimal sourceWeight, BigDecimal outputWeight,
                        Integer sourceWidth, Integer outputWidth,
                        WidthDifferencePolicy policy) {
        Integer plannedLossWidth() {
            if (policy != WidthDifferencePolicy.LOSS || sourceWidth == null || outputWidth == null
                    || outputWidth >= sourceWidth) return null;
            return sourceWidth - outputWidth;
        }

        BigDecimal plannedLossWeight() {
            if (policy != WidthDifferencePolicy.LOSS) return null;
            BigDecimal loss = sourceWeight.subtract(outputWeight);
            return loss.signum() > 0 ? loss : null;
        }
    }
}
