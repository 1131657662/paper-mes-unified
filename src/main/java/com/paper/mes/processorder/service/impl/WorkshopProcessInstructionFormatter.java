package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.calc.RewindWeightCalculator;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.ProcessStep;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class WorkshopProcessInstructionFormatter {

    private static final int VOID_ROLL_NO = 3;
    private static final int YES = 1;

    private WorkshopProcessInstructionFormatter() {
    }

    static String format(ProcessOrderDetailVO.RollProductionVO production) {
        if (production == null) return null;
        if (Integer.valueOf(FeeCalculator.STEP_TYPE_SAW).equals(production.getMainStepType())) {
            return formatSaw(production);
        }
        if (Integer.valueOf(FeeCalculator.STEP_TYPE_REWIND).equals(production.getMainStepType())) {
            return formatRewind(production);
        }
        return null;
    }

    private static String formatSaw(ProcessOrderDetailVO.RollProductionVO production) {
        int sourcePieces = sourcePieces(production);
        List<ProcessOrderDetailVO.FinishProductionVO> finishes = printable(production.getFinishes());
        String widths = widthPattern(finishes.stream().filter(item -> !isRemain(item)).toList(), sourcePieces);
        if (widths == null) return null;
        int trimWidth = trimWidth(finishes, sourcePieces) + plannedLossWidth(production.getSteps());
        String suffix = trimWidth > 0 ? "；每件切边余料" + trimWidth + "mm" : "";
        return "锯纸；每件成品" + widths + suffix + "。";
    }

    private static String formatRewind(ProcessOrderDetailVO.RollProductionVO production) {
        List<ProcessOrderDetailVO.RewindParamVO> params = perSourceParams(production);
        if (params.isEmpty()) return null;
        List<String> parts = new ArrayList<>();
        parts.add(rewindName(params));
        appendWeightSplit(parts, params);
        appendValues(parts, "成品门幅", values(params, ProcessOrderDetailVO.RewindParamVO::getLayerWidth), "mm");
        appendDiameter(parts, "目标直径", values(params, ProcessOrderDetailVO.RewindParamVO::getOutDiameter));
        appendCore(parts, values(params, ProcessOrderDetailVO.RewindParamVO::getCoreDiameter));
        return String.join("；", parts) + "。";
    }

    private static List<ProcessOrderDetailVO.RewindParamVO> perSourceParams(
            ProcessOrderDetailVO.RollProductionVO production) {
        List<ProcessOrderDetailVO.RewindParamVO> params = safe(production.getRewindParams());
        int pieces = sourcePieces(production);
        if (pieces > 1 && params.size() % pieces == 0) return params.subList(0, params.size() / pieces);
        return params;
    }

    private static void appendWeightSplit(List<String> parts, List<ProcessOrderDetailVO.RewindParamVO> params) {
        List<BigDecimal> ratios = params.stream().map(ProcessOrderDetailVO.RewindParamVO::getSplitRatio)
                .filter(Objects::nonNull).toList();
        if (ratios.size() < 2) return;
        String text = ratios.stream().map(WorkshopProcessInstructionFormatter::decimal)
                .map(value -> value + "%").collect(java.util.stream.Collectors.joining("+"));
        parts.add("每件按重量" + text + "分" + ratios.size() + "卷");
    }

    private static void appendValues(List<String> parts, String label, List<Integer> values, String unit) {
        if (values.isEmpty()) return;
        String text = counts(values).entrySet().stream().map(entry ->
                entry.getKey() + unit + (entry.getValue() > 1 ? "×" + entry.getValue() : ""))
                .collect(java.util.stream.Collectors.joining(" + "));
        parts.add(label + text);
    }

    private static void appendDiameter(List<String> parts, String label, List<Integer> values) {
        List<BigDecimal> converted = values.stream()
                .map(BigDecimal::valueOf)
                .map(RewindWeightCalculator::storedDiameterToMm)
                .toList();
        List<BigDecimal> distinct = converted.stream().distinct().toList();
        if (distinct.size() == 1) {
            parts.add(label + decimal(distinct.getFirst()) + "mm");
            return;
        }
        if (!converted.isEmpty()) {
            String text = counts(converted).entrySet().stream()
                    .map(entry -> decimal(entry.getKey()) + "mm"
                            + (entry.getValue() > 1 ? "×" + entry.getValue() : ""))
                    .collect(java.util.stream.Collectors.joining(" + "));
            parts.add(label + text);
        }
    }

    private static void appendCore(List<String> parts, List<Integer> values) {
        List<Integer> distinct = values.stream().distinct().toList();
        if (distinct.size() != 1) return;
        int core = distinct.getFirst();
        parts.add("纸芯" + core + (core <= 10 ? "英寸" : "mm"));
    }

    private static String rewindName(List<ProcessOrderDetailVO.RewindParamVO> params) {
        Integer mode = params.getFirst().getParamMode();
        return Integer.valueOf(6).equals(mode) ? "同规格复卷" : "复卷";
    }

    private static List<Integer> values(List<ProcessOrderDetailVO.RewindParamVO> params,
                                        java.util.function.Function<ProcessOrderDetailVO.RewindParamVO, Integer> getter) {
        return params.stream().map(getter).filter(Objects::nonNull).toList();
    }

    private static String widthPattern(List<ProcessOrderDetailVO.FinishProductionVO> finishes, int pieces) {
        List<Integer> widths = finishes.stream().map(ProcessOrderDetailVO.FinishProductionVO::getFinishWidth)
                .filter(Objects::nonNull).toList();
        if (widths.isEmpty()) return null;
        Map<Integer, Integer> normalized = new LinkedHashMap<>();
        counts(widths).forEach((width, count) -> normalized.put(width, normalizedCount(count, pieces)));
        return normalized.entrySet().stream().map(entry ->
                entry.getKey() + "mm" + (entry.getValue() > 1 ? "×" + entry.getValue() : ""))
                .collect(java.util.stream.Collectors.joining(" + "));
    }

    private static int trimWidth(List<ProcessOrderDetailVO.FinishProductionVO> finishes, int pieces) {
        int total = finishes.stream().filter(WorkshopProcessInstructionFormatter::isRemain)
                .map(ProcessOrderDetailVO.FinishProductionVO::getFinishWidth).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).sum();
        return normalizedCount(total, pieces);
    }

    private static int plannedLossWidth(List<ProcessStep> steps) {
        return safe(steps).stream().filter(step -> Integer.valueOf(YES).equals(step.getIsMain()))
                .map(ProcessStep::getPlannedLossWidth).filter(Objects::nonNull).findFirst().orElse(0);
    }

    private static List<ProcessOrderDetailVO.FinishProductionVO> printable(
            List<ProcessOrderDetailVO.FinishProductionVO> finishes) {
        return safe(finishes).stream().filter(item -> !Integer.valueOf(YES).equals(item.getIsSpare()))
                .filter(item -> !Integer.valueOf(VOID_ROLL_NO).equals(item.getRollNoStatus())).toList();
    }

    private static boolean isRemain(ProcessOrderDetailVO.FinishProductionVO item) {
        return Integer.valueOf(YES).equals(item.getIsRemain());
    }

    private static int sourcePieces(ProcessOrderDetailVO.RollProductionVO production) {
        return Math.max(1, production.getPieceNum() == null ? 1 : production.getPieceNum());
    }

    private static int normalizedCount(int count, int pieces) {
        return pieces > 1 && count % pieces == 0 ? count / pieces : count;
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static <T> Map<T, Integer> counts(List<T> values) {
        Map<T, Integer> result = new LinkedHashMap<>();
        values.forEach(value -> result.merge(value, 1, Integer::sum));
        return result;
    }

    private static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
