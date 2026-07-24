package com.paper.mes.report.subscription.dto;

import java.time.LocalDateTime;

public record ReportSubscriptionRunVO(
        String uuid,
        LocalDateTime scheduledFor,
        String metricReleaseUuid,
        Integer runStatus,
        Integer plannedCount,
        Integer dispatchedCount,
        Integer failedCount,
        String errorMessage,
        LocalDateTime completedAt
) {
}
