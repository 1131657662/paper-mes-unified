package com.paper.mes.health.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DataHealthSummaryVO(
        LocalDateTime checkedAt,
        long criticalCount,
        long warningCount,
        List<DataHealthIssueVO> issues
) {
}
