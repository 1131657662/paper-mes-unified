package com.paper.mes.report.dto;

import java.util.List;

public record ReportMetricReleaseDetailVO(
        ReportMetricReleaseSummaryVO release,
        List<ReportMetricVersionAuditVO> metrics
) {
}
