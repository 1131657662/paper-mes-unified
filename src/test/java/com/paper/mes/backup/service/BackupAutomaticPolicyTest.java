package com.paper.mes.backup.service;

import com.paper.mes.backup.dto.BackupRecordVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackupAutomaticPolicyTest {

    private BackupCatalog catalog;
    private BackupTaskHistoryService historyService;
    private BackupAutomaticPolicy policy;

    @BeforeEach
    void setUp() {
        BackupAutoSettingService settingService = mock(BackupAutoSettingService.class);
        catalog = mock(BackupCatalog.class);
        historyService = mock(BackupTaskHistoryService.class);
        BackupFeatureSettingService featureSetting = mock(BackupFeatureSettingService.class);
        BackupRuntimeResolver runtimeResolver = mock(BackupRuntimeResolver.class);
        policy = new BackupAutomaticPolicy(settingService, catalog, historyService,
                featureSetting, runtimeResolver);
        when(settingService.setting()).thenReturn(new BackupAutoSetting(true, LocalTime.of(2, 35)));
        when(featureSetting.isEnabled()).thenReturn(true);
        when(runtimeResolver.resolve()).thenReturn(runtime());
        when(historyService.automaticHistory()).thenReturn(new BackupAutomaticHistory(null, null, 0));
        when(catalog.list()).thenReturn(List.of());
    }

    @Test
    void isDue_beforeExecutionTime_returnsFalse() {
        assertFalse(policy.isDue(LocalDateTime.of(2026, 7, 13, 2, 34)));
    }

    @Test
    void isDue_afterExecutionTime_withoutBackup_returnsTrue() {
        assertTrue(policy.isDue(LocalDateTime.of(2026, 7, 13, 2, 36)));
    }

    @Test
    void isDue_withValidBackupToday_returnsFalse() {
        when(catalog.list()).thenReturn(List.of(validBackup(LocalDateTime.of(2026, 7, 13, 2, 30))));

        assertFalse(policy.isDue(LocalDateTime.of(2026, 7, 13, 2, 36)));
    }

    @Test
    void isDue_afterFailedAttemptToday_returnsFalse() {
        when(historyService.automaticHistory()).thenReturn(new BackupAutomaticHistory(
                LocalDateTime.of(2026, 7, 13, 2, 35), "FAILED", 1));

        assertFalse(policy.isDue(LocalDateTime.of(2026, 7, 13, 3, 0)));
    }

    @Test
    void isDue_dayAfterFailedAttempt_returnsTrue() {
        when(historyService.automaticHistory()).thenReturn(new BackupAutomaticHistory(
                LocalDateTime.of(2026, 7, 12, 2, 35), "FAILED", 1));

        assertTrue(policy.isDue(LocalDateTime.of(2026, 7, 13, 3, 0)));
    }

    private BackupRuntime runtime() {
        return new BackupRuntime("WINDOWS", "POWERSHELL", null, null, null, null, List.of());
    }

    private BackupRecordVO validBackup(LocalDateTime createdAt) {
        return BackupRecordVO.builder().id("20260713-023000").createdAt(createdAt)
                .databaseArchive(true).checksumAvailable(true).build();
    }
}
