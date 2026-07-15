package com.paper.mes.backup.service;

import com.paper.mes.backup.config.BackupProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupRuntimeResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolve_withExplicitPaths_preservesOverrides() throws Exception {
        Path backupScript = Files.createFile(tempDir.resolve("backup-script"));
        Path verifyScript = Files.createFile(tempDir.resolve("verify-script"));
        BackupProperties properties = new BackupProperties();
        properties.setRootDir(tempDir.resolve("backups").toString());
        properties.setBackupScript(backupScript.toString());
        properties.setVerifyScript(verifyScript.toString());

        BackupRuntime runtime = new BackupRuntimeResolver(properties).resolve();

        assertEquals(backupScript, runtime.backupScript());
        assertEquals(verifyScript, runtime.verifyScript());
        assertEquals(tempDir.resolve("backups"), runtime.root());
        assertTrue(runtime.platform().equals("WINDOWS") || runtime.platform().equals("LINUX"));
    }
}
