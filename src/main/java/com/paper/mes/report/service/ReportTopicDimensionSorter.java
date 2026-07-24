package com.paper.mes.report.service;

import com.paper.mes.report.dto.ReportDimensionVO;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public final class ReportTopicDimensionSorter {
    private ReportTopicDimensionSorter() {
    }

    public static List<ReportDimensionVO> byInputWeight(List<ReportDimensionVO> rows) {
        return rows.stream().sorted(descending(ReportDimensionVO::getOriginalWeight)).toList();
    }

    public static List<ReportDimensionVO> byLossRisk(List<ReportDimensionVO> rows) {
        return rows.stream().sorted(Comparator
                .comparing((ReportDimensionVO row) -> value(row.getLossRatio())).reversed()
                .thenComparing(row -> value(row.getLossWeight()), Comparator.reverseOrder())
                .thenComparing(ReportTopicDimensionSorter::name)).toList();
    }

    private static Comparator<ReportDimensionVO> descending(
            java.util.function.Function<ReportDimensionVO, BigDecimal> value) {
        return Comparator.comparing((ReportDimensionVO row) -> ReportTopicDimensionSorter.value(value.apply(row)))
                .reversed().thenComparing(ReportTopicDimensionSorter::name);
    }

    private static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String name(ReportDimensionVO row) {
        return row.getDimensionName() == null ? "" : row.getDimensionName();
    }
}
