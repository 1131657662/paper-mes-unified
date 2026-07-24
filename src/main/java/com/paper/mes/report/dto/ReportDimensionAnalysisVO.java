package com.paper.mes.report.dto;

import java.util.List;

public record ReportDimensionAnalysisVO(
        ReportQueryExecutionMetaVO execution,
        List<ReportDimensionVO> rows
) {
}
