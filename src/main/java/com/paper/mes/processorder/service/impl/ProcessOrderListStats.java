package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.service.FinishRollStatusPolicy;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ProcessOrderListStats {

    private static final int PROCESS_MODE_DIRECT_SHIP = 3;
    private static final int IS_SPARE_NO = 0;
    private static final int IS_SPARE_YES = 1;
    private static final int IS_REMAIN_YES = 1;
    private static final int ROLL_NO_VOID = 3;

    private ProcessOrderListStats() {
    }

    static void apply(ProcessOrder order, List<OriginalRoll> originals, List<FinishRoll> finishes) {
        List<OriginalRoll> originalRows = originals == null ? List.of() : originals;
        List<FinishRoll> finishRows = finishes == null ? List.of() : finishes;
        order.setOriginalRollCount(originalRows.size());
        order.setOriginalPieceCount(sumOriginalPieces(originalRows));
        order.setOriginalRollWeight(sumOriginalWeight(originalRows));
        order.setFinishRollCount((int) finishRows.stream().filter(ProcessOrderListStats::isFormalFinishRoll).count());
        order.setFinishRollWeight(sumFinishWeight(finishRows));
        order.setEstimateFinishWeight(sumFinishEstimateWeight(finishRows));
        order.setActualFinishWeight(sumFinishActualWeight(finishRows));
        order.setSpareRollCount((int) finishRows.stream().filter(ProcessOrderListStats::isActiveSpareRoll).count());
    }

    static List<String> processNames(List<ProcessStep> steps, List<OriginalRoll> rolls) {
        List<ProcessStep> rows = steps == null ? List.of() : steps;
        if (rows.isEmpty()) {
            return allDirectShip(rolls) ? List.of("直发") : List.of("待配置");
        }
        Map<Integer, String> namesByType = new LinkedHashMap<>();
        for (ProcessStep step : rows) {
            if (step.getStepType() == null) continue;
            String name = StringUtils.hasText(step.getStepName())
                    ? step.getStepName().trim()
                    : processStepName(step.getStepType());
            namesByType.putIfAbsent(step.getStepType(), name);
        }
        return List.copyOf(namesByType.values());
    }

    static boolean isFormalFinishRoll(FinishRoll roll) {
        boolean formal = roll.getIsSpare() == null || roll.getIsSpare() == IS_SPARE_NO;
        boolean finalProduct = roll.getIsRemain() == null || roll.getIsRemain() != IS_REMAIN_YES;
        boolean active = roll.getRollNoStatus() == null || roll.getRollNoStatus() != ROLL_NO_VOID;
        return formal && finalProduct && active && !FinishRollStatusPolicy.isScrapped(roll);
    }

    private static boolean allDirectShip(List<OriginalRoll> rolls) {
        return rolls != null && !rolls.isEmpty()
                && rolls.stream().allMatch(roll -> Integer.valueOf(PROCESS_MODE_DIRECT_SHIP)
                .equals(roll.getProcessMode()));
    }

    private static int sumOriginalPieces(List<OriginalRoll> rolls) {
        return rolls.stream().mapToInt(roll -> roll.getPieceNum() == null ? 1 : roll.getPieceNum()).sum();
    }

    private static BigDecimal sumOriginalWeight(List<OriginalRoll> rolls) {
        return rolls.stream()
                .map(roll -> roll.getActualWeight() == null ? roll.getTotalWeight() : roll.getActualWeight())
                .map(ProcessOrderListStats::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumFinishWeight(List<FinishRoll> rolls) {
        return rolls.stream()
                .filter(ProcessOrderListStats::isFormalFinishRoll)
                .map(roll -> roll.getActualWeight() == null ? roll.getEstimateWeight() : roll.getActualWeight())
                .map(ProcessOrderListStats::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumFinishEstimateWeight(List<FinishRoll> rolls) {
        return rolls.stream()
                .filter(ProcessOrderListStats::isFormalFinishRoll)
                .map(FinishRoll::getEstimateWeight)
                .map(ProcessOrderListStats::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumFinishActualWeight(List<FinishRoll> rolls) {
        return rolls.stream()
                .filter(ProcessOrderListStats::isFormalFinishRoll)
                .map(FinishRoll::getActualWeight)
                .map(ProcessOrderListStats::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean isActiveSpareRoll(FinishRoll roll) {
        return Integer.valueOf(IS_SPARE_YES).equals(roll.getIsSpare())
                && !Integer.valueOf(ROLL_NO_VOID).equals(roll.getRollNoStatus())
                && !FinishRollStatusPolicy.isScrapped(roll);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String processStepName(Integer stepType) {
        if (stepType == null) return "其他工艺";
        return switch (stepType) {
            case 1 -> "锯纸";
            case 2 -> "复卷";
            case 3 -> "剥损整理";
            case 4 -> "重新包装";
            default -> "其他工艺";
        };
    }
}
