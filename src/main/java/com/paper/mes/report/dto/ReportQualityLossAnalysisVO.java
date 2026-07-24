package com.paper.mes.report.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReportQualityLossAnalysisVO(
        String topicCode,
        ReportOverviewVO overview,
        List<ReportDimensionVO> monthlyTrend,
        List<ReportDimensionVO> paperBreakdown,
        List<ReportDetailVO> lossLeaders,
        LocalDateTime asOf
) {
}
