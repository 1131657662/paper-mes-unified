package com.paper.mes.exporttask.service;

public record ExportTaskExecutorSnapshot(
        int workerCount,
        int activeWorkerCount,
        int queuedInMemoryCount,
        int queueCapacity,
        long rejectedSubmissionCount,
        long completedExecutionCount
) {
}
