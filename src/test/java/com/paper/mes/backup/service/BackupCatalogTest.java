package com.paper.mes.backup.service;

import com.paper.mes.backup.config.BackupProperties;
import com.paper.mes.backup.dto.BackupRecordVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupCatalogTest {

    @TempDir
    Path tempDir;

    @Test
    void list_withValidBackup_returnsMetadata() throws Exception {
        BackupCatalog catalog = catalog();
        Path backup = Files.createDirectory(tempDir.resolve("20260713-023000"));
        Files.writeString(backup.resolve("paper_processing.sql.gz"), "database");
        Files.writeString(backup.resolve("upload.tar.gz"), "uploads");
        Files.writeString(backup.resolve("SHA256SUMS"), "checksums");
        Files.writeString(backup.resolve("restore-check.txt"),
                "verified_at=2026-07-13T04:30:00+08:00\n");

        List<BackupRecordVO> records = catalog.list();

        assertEquals(1, records.size());
        assertEquals("20260713-023000", records.getFirst().getId());
        assertEquals("VERIFIED", records.getFirst().getVerificationStatus());
        assertEquals("COMPLETE", records.getFirst().getIntegrityStatus());
        assertTrue(records.getFirst().isDatabaseArchive());
        assertTrue(records.getFirst().isUploadIncluded());
    }

    @Test
    void requireBackup_withTraversal_rejectsInput() {
        BackupCatalog catalog = catalog();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> catalog.requireBackup("../20260713-023000"));

        assertEquals("备份编号格式不正确", error.getMessage());
    }

    @Test
    void list_withUnrelatedDirectory_ignoresDirectory() throws Exception {
        BackupCatalog catalog = catalog();
        Files.createDirectory(tempDir.resolve("manual-copy"));

        assertTrue(catalog.list().isEmpty());
    }

    @Test
    void list_withEmptyBackupDirectory_marksMissingRequiredFiles() throws Exception {
        BackupCatalog catalog = catalog();
        Files.createDirectory(tempDir.resolve("20260713-023000"));

        BackupRecordVO record = catalog.list().getFirst();

        assertEquals("INCOMPLETE", record.getIntegrityStatus());
        assertEquals(List.of("数据库文件", "校验文件"), record.getMissingItems());
        assertEquals(0L, record.getSizeBytes());
    }

    @Test
    void list_withEmptyRequiredFiles_marksBackupIncomplete() throws Exception {
        BackupCatalog catalog = catalog();
        Path backup = Files.createDirectory(tempDir.resolve("20260713-023000"));
        Files.createFile(backup.resolve("paper_processing.sql.gz"));
        Files.createFile(backup.resolve("SHA256SUMS"));

        BackupRecordVO record = catalog.list().getFirst();

        assertEquals("INCOMPLETE", record.getIntegrityStatus());
        assertEquals(List.of("数据库文件", "校验文件"), record.getMissingItems());
    }

    @Test
    void list_withTemporaryDirectory_ignoresDirectory() throws Exception {
        BackupCatalog catalog = catalog();
        Files.createDirectory(tempDir.resolve(".tmp-20260713-023000-123"));

        assertTrue(catalog.list().isEmpty());
    }

    @Test
    void requireVerifiableBackup_withIncompleteDirectory_rejectsRequest() throws Exception {
        BackupCatalog catalog = catalog();
        Files.createDirectory(tempDir.resolve("20260713-023000"));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> catalog.requireVerifiableBackup("20260713-023000"));

        assertEquals("备份不完整，无法进行隔离恢复验证", error.getMessage());
    }

    private BackupCatalog catalog() {
        BackupProperties properties = new BackupProperties();
        properties.setRootDir(tempDir.toString());
        properties.setSourceDbName("paper_processing");
        return new BackupCatalog(properties, new BackupRuntimeResolver(properties));
    }
}
