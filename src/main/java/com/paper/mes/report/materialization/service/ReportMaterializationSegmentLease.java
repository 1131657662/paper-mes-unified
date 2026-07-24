package com.paper.mes.report.materialization.service;

import java.time.LocalDateTime;

public record ReportMaterializationSegmentLease(
        String segmentUuid,
        String leaseOwner,
        long fencingToken,
        LocalDateTime leaseUntil
) {
}
