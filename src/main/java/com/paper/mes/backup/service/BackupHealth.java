package com.paper.mes.backup.service;

public record BackupHealth(
        long totalSpaceBytes,
        long usableSpaceBytes,
        int retentionDays,
        OffsiteBackupStatus offsite) {
}
