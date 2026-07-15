package com.paper.mes.backup.service;

import com.paper.mes.backup.config.BackupProperties;
import com.paper.mes.common.BusinessException;
import com.paper.mes.oplog.service.OperationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackupMaintenanceServiceTest {

    @TempDir
    Path tempDir;

    private BackupMaintenanceService service;

    @BeforeEach
    void setUp() {
        BackupRetentionSettingService retentionSetting = mock(BackupRetentionSettingService.class);
        when(retentionSetting.retentionDays()).thenReturn(30);
        service = new BackupMaintenanceService(catalog(), retentionSetting,
                mock(OperationLogService.class), new BackupOperationGuard(),
                mock(OffsiteBackupStatusReader.class));
    }

    @Test
    void delete_withLastValidBackup_rejectsDeletion() throws Exception {
        Path backup = createBackup("20260713-023000", true);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.delete("20260713-023000", "admin"));

        assertEquals("至少需要保留一份有效备份，当前备份不能删除", error.getMessage());
        assertTrue(Files.exists(backup));
    }

    @Test
    void delete_withTwoValidBackups_deletesSelectedBackup() throws Exception {
        Path selected = createBackup("20260712-023000", true);
        Path retained = createBackup("20260713-023000", true);

        service.delete("20260712-023000", "admin");

        assertFalse(Files.exists(selected));
        assertTrue(Files.exists(retained));
    }

    @Test
    void delete_withLastVerifiedBackup_rejectsEvenWhenUnverifiedBackupExists() throws Exception {
        Path verified = createBackup("20260712-023000", true);
        Files.writeString(verified.resolve("restore-check.txt"), "verified_at=2026-07-12T03:00:00+08:00");
        createBackup("20260713-023000", true);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.delete("20260712-023000", "admin"));

        assertEquals("至少需要保留一份恢复验证通过的备份，当前备份不能删除", error.getMessage());
        assertTrue(Files.exists(verified));
    }

    @Test
    void cleanupExpired_withOnlyExpiredValidBackups_retainsNewestValidBackup() throws Exception {
        Path older = createBackup("20000101-000000", true);
        Path newest = createBackup("20000102-000000", true);

        int deleted = service.cleanupExpired("system");

        assertEquals(1, deleted);
        assertFalse(Files.exists(older));
        assertTrue(Files.exists(newest));
    }

    @Test
    void cleanupExpired_withNewerDamagedBackup_protectsOlderValidBackup() throws Exception {
        Path valid = createBackup("20000101-000000", true);
        Path damaged = createBackup("20000102-000000", false);

        int deleted = service.cleanupExpired("system");

        assertEquals(1, deleted);
        assertTrue(Files.exists(valid));
        assertFalse(Files.exists(damaged));
    }

    @Test
    void cleanupExpired_prefersVerifiedBackupOverNewerUnverifiedBackup() throws Exception {
        Path verified = createBackup("20000101-000000", true);
        Files.writeString(verified.resolve("restore-check.txt"), "verified_at=2000-01-01T03:00:00+08:00");
        Path unverified = createBackup("20000102-000000", true);

        int deleted = service.cleanupExpired("system");

        assertEquals(1, deleted);
        assertTrue(Files.exists(verified));
        assertFalse(Files.exists(unverified));
    }

    private Path createBackup(String backupId, boolean checksumAvailable) throws Exception {
        Path backup = Files.createDirectory(tempDir.resolve(backupId));
        Files.writeString(backup.resolve("paper_processing.sql.gz"), "database");
        if (checksumAvailable) {
            Files.writeString(backup.resolve("SHA256SUMS"), "checksums");
        }
        return backup;
    }

    private BackupCatalog catalog() {
        BackupProperties properties = new BackupProperties();
        properties.setRootDir(tempDir.toString());
        properties.setSourceDbName("paper_processing");
        return new BackupCatalog(properties, new BackupRuntimeResolver(properties));
    }
}
