package com.paper.mes.report.subscription.dto;

import java.util.List;

public record ReportSubscriptionRunPageVO(
        List<ReportSubscriptionRunVO> records,
        long total,
        long current,
        long size
) {
}
