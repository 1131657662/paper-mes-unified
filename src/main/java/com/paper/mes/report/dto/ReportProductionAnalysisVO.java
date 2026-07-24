package com.paper.mes.report.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReportProductionAnalysisVO(
        String topicCode,
        ReportOverviewVO overview,
        List<ReportDimensionVO> monthlyTrend,
        List<ReportDimensionVO> processBreakdown,
        List<ReportDimensionVO> machineBreakdown,
        LocalDateTime asOf,
        ReportQueryExecutionMetaVO execution
) {
}
