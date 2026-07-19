package com.paper.mes.exporttask.dto;

import com.paper.mes.exporttask.entity.ExportTask;

import java.time.LocalDateTime;

public record ExportTaskVO(String uuid, String taskType, String moduleCode, String operationCode,
                           String taskName, String sourceUuid,
                           Integer taskStatus, Integer progress, String fileName, Long fileSize,
                           String errorMessage, LocalDateTime createTime, LocalDateTime completedAt,
                           LocalDateTime expiresAt, LocalDateTime downloadedAt, Integer downloadCount,
                           Integer attemptCount, Integer maxAttempts, boolean acknowledged,
                           boolean resourceAccessible) {
    public static ExportTaskVO from(ExportTask task, boolean resourceAccessible) {
        return new ExportTaskVO(task.getUuid(), task.getTaskType(), task.getModuleCode(), task.getOperationCode(),
                task.getTaskName(), task.getSourceUuid(),
                task.getTaskStatus(), task.getProgress(), task.getFileName(), task.getFileSize(),
                task.getErrorMessage(), task.getCreateTime(), task.getCompletedAt(), task.getExpiresAt(),
                task.getDownloadedAt(), task.getDownloadCount(), task.getAttemptCount(), task.getMaxAttempts(),
                task.getAcknowledgedAt() != null, resourceAccessible);
    }
}
