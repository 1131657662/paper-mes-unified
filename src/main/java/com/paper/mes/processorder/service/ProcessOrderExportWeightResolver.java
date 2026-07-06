package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.FinishRoll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ProcessOrderExportWeightResolver {

    private ProcessOrderExportWeightResolver() {
    }

    static BigDecimal estimateWeight(FinishRoll finish, Map<String, BigDecimal> fallbackWeights) {
        BigDecimal explicit = ProcessOrderExportText.estimateWeight(finish);
        if (isPositive(explicit)) {
            return explicit;
        }
        BigDecimal byUuid = fallbackWeights.get(finish.getUuid());
        if (byUuid != null) {
            return byUuid;
        }
        return fallbackWeights.get(finish.getFinishRollNo());
    }

    static Map<String, BigDecimal> fallbackEstimateWeights(List<ProcessOrderDetailVO.RollProductionVO> productions) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (productions == null) {
            return result;
        }
        for (ProcessOrderDetailVO.RollProductionVO production : productions) {
            appendProductionEstimateWeights(result, production);
        }
        return result;
    }

    private static void appendProductionEstimateWeights(Map<String, BigDecimal> result,
                                                        ProcessOrderDetailVO.RollProductionVO production) {
        List<ProcessOrderDetailVO.FinishProductionVO> finishes = production.getFinishes() == null
                ? List.of()
                : production.getFinishes();
        List<ProcessOrderDetailVO.FinishProductionVO> official = officialFinishes(finishes);
        for (ProcessOrderDetailVO.FinishProductionVO finish : finishes) {
            BigDecimal estimate = resolveEstimateWeight(production, finish, finishes, official);
            putEstimate(result, finish.getUuid(), estimate);
            putEstimate(result, finish.getFinishRollNo(), estimate);
        }
    }

    private static List<ProcessOrderDetailVO.FinishProductionVO> officialFinishes(
            List<ProcessOrderDetailVO.FinishProductionVO> finishes) {
        return finishes.stream()
                .filter(finish -> (finish.getIsSpare() == null || finish.getIsSpare() != 1)
                        && (finish.getIsRemain() == null || finish.getIsRemain() != 1))
                .toList();
    }

    private static BigDecimal resolveEstimateWeight(ProcessOrderDetailVO.RollProductionVO production,
                                                    ProcessOrderDetailVO.FinishProductionVO finish,
                                                    List<ProcessOrderDetailVO.FinishProductionVO> allFinishes,
                                                    List<ProcessOrderDetailVO.FinishProductionVO> official) {
        if (isPositive(finish.getEstimateWeight())) {
            return finish.getEstimateWeight();
        }
        if (isNonDeliverable(finish) || official.isEmpty()) {
            return null;
        }
        BigDecimal available = productionAvailableWeight(production, allFinishes);
        BigDecimal widthBasis = finishWidthBasis(official);
        if (isPositive(widthBasis) && finish.getFinishWidth() != null) {
            return available.multiply(BigDecimal.valueOf(finish.getFinishWidth()))
                    .divide(widthBasis, 3, RoundingMode.HALF_UP);
        }
        return available.divide(BigDecimal.valueOf(official.size()), 3, RoundingMode.HALF_UP);
    }

    private static BigDecimal productionAvailableWeight(ProcessOrderDetailVO.RollProductionVO production,
                                                        List<ProcessOrderDetailVO.FinishProductionVO> finishes) {
        BigDecimal rollWeight = production.getRollWeight() == null ? BigDecimal.ZERO : production.getRollWeight();
        BigDecimal pieces = BigDecimal.valueOf(production.getPieceNum() == null ? 1 : production.getPieceNum());
        BigDecimal trimWeight = finishes.stream()
                .filter(ProcessOrderExportWeightResolver::countsTowardLoss)
                .map(ProcessOrderExportWeightResolver::trimWeight)
                .filter(ProcessOrderExportWeightResolver::isPositive)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal available = rollWeight.multiply(pieces).subtract(trimWeight);
        return available.signum() < 0 ? BigDecimal.ZERO : available;
    }

    private static boolean countsTowardLoss(ProcessOrderDetailVO.FinishProductionVO finish) {
        return (finish.getIsSpare() == null || finish.getIsSpare() != 1)
                && (finish.getRollNoStatus() == null || finish.getRollNoStatus() != 3);
    }

    private static BigDecimal trimWeight(ProcessOrderDetailVO.FinishProductionVO finish) {
        if (finish.getIsRemain() != null && finish.getIsRemain() == 1) {
            BigDecimal actual = finish.getActualWeight();
            return isPositive(actual) ? actual : finish.getEstimateWeight();
        }
        return finish.getTrimWeightShare();
    }

    private static BigDecimal finishWidthBasis(List<ProcessOrderDetailVO.FinishProductionVO> finishes) {
        return finishes.stream()
                .map(ProcessOrderDetailVO.FinishProductionVO::getFinishWidth)
                .filter(width -> width != null && width > 0)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void putEstimate(Map<String, BigDecimal> result, String key, BigDecimal estimate) {
        if (key != null && !key.isBlank() && estimate != null) {
            result.put(key, estimate);
        }
    }

    private static boolean isNonDeliverable(ProcessOrderDetailVO.FinishProductionVO finish) {
        return (finish.getIsSpare() != null && finish.getIsSpare() == 1)
                || (finish.getIsRemain() != null && finish.getIsRemain() == 1);
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
