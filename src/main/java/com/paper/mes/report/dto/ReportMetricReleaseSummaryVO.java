package com.paper.mes.report.dto;

import java.time.LocalDateTime;

public record ReportMetricReleaseSummaryVO(
        String releaseUuid,
        String releaseCode,
        String releaseName,
        int releaseStatus,
        String releaseChecksum,
        long metricCount,
        LocalDateTime publishedAt,
        String publishedBy,
        LocalDateTime retiredAt,
        String retiredBy,
        LocalDateTime createTime,
        LocalDateTime asOf
) {
}
