package com.paper.mes.report.materialization.dto;

import java.time.LocalDate;

public record ReportMaterializationJobRequest(
        String taskId,
        String metricReleaseUuid,
        LocalDate periodStart,
        LocalDate periodEnd,
        String requestedBy
) {
}
