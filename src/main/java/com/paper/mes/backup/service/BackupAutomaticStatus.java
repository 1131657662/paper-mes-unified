package com.paper.mes.backup.service;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record BackupAutomaticStatus(boolean enabled, LocalTime executionTime,
                                    LocalDateTime lastStartedAt, String lastStatus,
                                    long consecutiveFailures, LocalDateTime nextExecutionAt) {
}
