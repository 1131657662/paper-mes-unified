package com.paper.mes.report.materialization.service;

public record ReportMetricPublicationCommand(
        String taskId,
        String segmentUuid,
        String workerId,
        long fencingToken,
        String generationUuid
) {
}
