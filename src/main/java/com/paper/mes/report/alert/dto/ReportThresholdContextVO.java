package com.paper.mes.report.alert.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReportThresholdContextVO(
        LocalDateTime asOf,
        List<ReportThresholdItemVO> thresholds
) {
}
