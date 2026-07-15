package com.paper.mes.backup.service;

import java.time.LocalDateTime;

public record BackupAutomaticHistory(LocalDateTime lastStartedAt, String lastStatus,
                                     long consecutiveFailures) {
}
