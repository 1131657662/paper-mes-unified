package com.paper.mes.backup.service;

import com.paper.mes.backup.config.BackupProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledOnOs(OS.LINUX)
class BackupCommandRunnerLinuxTest {

    @TempDir
    Path tempDir;

    @Test
    void backup_withVerifyWrapperConfigured_stillRunsBackupScriptDirectly() throws Exception {
        Path script = tempDir.resolve("backup.sh");
        Files.writeString(script, "printf 'backup_id=20260816-073000\\n'\n");
        BackupProperties properties = new BackupProperties();
        properties.setCommandTimeout(Duration.ofSeconds(5));
        properties.setVerifyWrapper("/usr/local/sbin/verify-paper-mes-backup-root");

        BackupRuntime runtime = new BackupRuntime("LINUX", "BASH", tempDir,
                script, script, tempDir.resolve("missing.env"), List.of());
        BackupRuntimeResolver resolver = mock(BackupRuntimeResolver.class);
        when(resolver.resolve()).thenReturn(runtime);
        BackupProcessEnvironment environment = mock(BackupProcessEnvironment.class);
        when(environment.variables()).thenReturn(Map.of());

        String backupId = new BackupCommandRunner(properties, resolver, environment).backup(tempDir);

        assertEquals("20260816-073000", backupId);
    }
}
