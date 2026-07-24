package com.paper.mes.report.materialization.service;

import java.time.LocalDateTime;

public record ReportMaterializationLease(
        String jobUuid,
        String workerId,
        long fencingToken,
        LocalDateTime leaseUntil
) {
}
