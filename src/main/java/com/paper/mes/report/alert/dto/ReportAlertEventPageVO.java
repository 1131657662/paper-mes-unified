package com.paper.mes.report.alert.dto;

import java.util.List;

public record ReportAlertEventPageVO(
        List<ReportAlertEventVO> items,
        long total,
        int page,
        int size,
        long activeCount,
        long resolvedCount,
        long ignoredCount
) {
}
