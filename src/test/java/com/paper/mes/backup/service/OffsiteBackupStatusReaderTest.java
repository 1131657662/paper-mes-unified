package com.paper.mes.backup.service;

import com.paper.mes.backup.config.BackupProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OffsiteBackupStatusReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void read_withValidStatus_returnsSanitizedSyncResult() throws Exception {
        Files.writeString(tempDir.resolve(".remote-sync-status"), """
                version=1
                status=SUCCESS
                completed_at=2026-07-13T03:15:00Z
                remote_name=paper_mes_archive
                """);

        OffsiteBackupStatus status = reader().read();

        assertEquals(OffsiteBackupStatus.State.SUCCESS, status.state());
        assertEquals("paper_mes_archive", status.remoteName());
        assertEquals(11, status.lastSyncAt().getHour());
    }

    @Test
    void read_withUnsafeRemoteName_returnsInvalidStatus() throws Exception {
        Files.writeString(tempDir.resolve(".remote-sync-status"), """
                version=1
                status=FAILED
                completed_at=2026-07-13T03:15:00Z
                remote_name=remote:path
                """);

        OffsiteBackupStatus status = reader().read();

        assertEquals(OffsiteBackupStatus.State.INVALID, status.state());
        assertNull(status.remoteName());
    }

    @Test
    void read_withoutStatusFile_returnsNotConfigured() {
        assertEquals(OffsiteBackupStatus.State.NOT_CONFIGURED, reader().read().state());
    }

    @Test
    void read_withOversizedStatusFile_returnsInvalid() throws Exception {
        Files.writeString(tempDir.resolve(".remote-sync-status"), "x".repeat(4097));

        assertEquals(OffsiteBackupStatus.State.INVALID, reader().read().state());
    }

    @Test
    void read_withUnsupportedVersion_returnsInvalid() throws Exception {
        Files.writeString(tempDir.resolve(".remote-sync-status"), """
                version=2
                status=SUCCESS
                completed_at=2026-07-13T03:15:00Z
                remote_name=paper_mes_archive
                """);

        assertEquals(OffsiteBackupStatus.State.INVALID, reader().read().state());
    }

    private OffsiteBackupStatusReader reader() {
        BackupProperties properties = new BackupProperties();
        properties.setRootDir(tempDir.toString());
        BackupRuntimeResolver runtimeResolver = new BackupRuntimeResolver(properties);
        return new OffsiteBackupStatusReader(new BackupCatalog(properties, runtimeResolver));
    }
}
