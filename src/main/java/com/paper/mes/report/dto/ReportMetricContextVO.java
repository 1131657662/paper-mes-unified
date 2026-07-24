package com.paper.mes.report.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReportMetricContextVO(
        String releaseUuid,
        String releaseCode,
        String releaseName,
        String releaseChecksum,
        LocalDateTime publishedAt,
        LocalDateTime asOf,
        List<ReportMetricItemVO> metrics
) {
}
