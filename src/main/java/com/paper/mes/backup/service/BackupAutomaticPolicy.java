package com.paper.mes.backup.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class BackupAutomaticPolicy {

    private final BackupAutoSettingService settingService;
    private final BackupCatalog catalog;
    private final BackupTaskHistoryService historyService;
    private final BackupFeatureSettingService featureSettingService;
    private final BackupRuntimeResolver runtimeResolver;

    public BackupAutomaticPolicy(BackupAutoSettingService settingService, BackupCatalog catalog,
                                 BackupTaskHistoryService historyService,
                                 BackupFeatureSettingService featureSettingService,
                                 BackupRuntimeResolver runtimeResolver) {
        this.settingService = settingService;
        this.catalog = catalog;
        this.historyService = historyService;
        this.featureSettingService = featureSettingService;
        this.runtimeResolver = runtimeResolver;
    }

    public BackupAutomaticStatus status(LocalDateTime now) {
        BackupAutoSetting setting = settingService.setting();
        BackupAutomaticHistory history = historyService.automaticHistory();
        boolean available = setting.enabled() && featureSettingService.isEnabled()
                && runtimeResolver.resolve().configured();
        LocalDateTime next = available ? nextExecution(now, setting, history) : null;
        return new BackupAutomaticStatus(setting.enabled(), setting.executionTime(),
                history.lastStartedAt(), history.lastStatus(), history.consecutiveFailures(), next);
    }

    public boolean isDue(LocalDateTime now) {
        BackupAutoSetting setting = settingService.setting();
        if (!setting.enabled() || !featureSettingService.isEnabled()) return false;
        if (!runtimeResolver.resolve().configured()) return false;
        LocalDateTime scheduled = now.toLocalDate().atTime(setting.executionTime());
        if (now.isBefore(scheduled) || hasValidBackup(now.toLocalDate())) return false;
        return notAttemptedToday(now, historyService.automaticHistory());
    }

    private LocalDateTime nextExecution(LocalDateTime now, BackupAutoSetting setting,
                                        BackupAutomaticHistory history) {
        LocalDateTime scheduled = now.toLocalDate().atTime(setting.executionTime());
        if (now.isBefore(scheduled)) return scheduled;
        if (!hasValidBackup(now.toLocalDate()) && notAttemptedToday(now, history)) return now;
        return scheduled.plusDays(1);
    }

    private boolean notAttemptedToday(LocalDateTime now, BackupAutomaticHistory history) {
        return history.lastStartedAt() == null
                || !history.lastStartedAt().toLocalDate().equals(now.toLocalDate());
    }

    private boolean hasValidBackup(LocalDate date) {
        return catalog.list().stream().anyMatch(record -> record.isDatabaseArchive()
                && record.isChecksumAvailable() && record.getCreatedAt().toLocalDate().equals(date));
    }
}
