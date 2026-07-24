package com.paper.mes.report.subscription.dto;

import com.paper.mes.report.dto.ReportQuery;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ReportSubscriptionVO(
        String uuid,
        String subscriptionName,
        String reportPath,
        Integer scheduleType,
        LocalTime executionTime,
        Integer weekDay,
        Integer monthDay,
        String timezone,
        ReportQuery reportQuery,
        Integer periodPolicy,
        Integer releasePolicy,
        String pinnedReleaseUuid,
        Integer isEnabled,
        LocalDateTime nextRunAt,
        LocalDateTime lastScheduledAt,
        String lastErrorMessage,
        Integer version,
        List<ReportSubscriptionRecipientVO> recipients
) {
}
