package com.paper.mes.report.dto;

import com.paper.mes.common.PageResult;

import java.util.List;

public record ReportPageAnalysisVO(
        ReportQueryExecutionMetaVO execution,
        ReportOverviewVO overview,
        List<ReportDimensionVO> currentBreakdown,
        PageResult<ReportDetailVO> details,
        List<ReportDimensionVO> monthlyTrend,
        List<ReportDimensionVO> customerRanking,
        List<ReportDimensionVO> paperRanking
) {
}
