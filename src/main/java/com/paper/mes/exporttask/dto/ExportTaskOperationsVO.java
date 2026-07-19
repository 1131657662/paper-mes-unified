package com.paper.mes.exporttask.dto;

import java.time.LocalDateTime;

public record ExportTaskOperationsVO(
        long queuedCount,
        long runningCount,
        long succeededLast24Hours,
        long failedLast24Hours,
        long staleRunningCount,
        LocalDateTime oldestQueuedAt,
        double averageDurationSeconds,
        long storedFileBytes,
        long sseConnectionCount,
        int workerCount,
        int activeWorkerCount,
        int queuedInMemoryCount,
        int queueCapacity,
        long rejectedSubmissionCount,
        long completedExecutionCount,
        String storageStatus,
        boolean storageAvailable,
        boolean storageWritable,
        long storageFreeBytes,
        long storageTotalBytes,
        double storageFreePercent,
        LocalDateTime storageCheckedAt,
        LocalDateTime asOf
) {
}
