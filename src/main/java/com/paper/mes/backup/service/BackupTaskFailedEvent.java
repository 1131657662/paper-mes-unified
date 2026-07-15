package com.paper.mes.backup.service;

import java.time.LocalDateTime;

public record BackupTaskFailedEvent(
        String taskUuid,
        String taskType,
        String backupId,
        LocalDateTime startedAt) {
}
