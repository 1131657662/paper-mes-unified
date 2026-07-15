package com.paper.mes.backup.service;

import com.paper.mes.backup.config.BackupProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledOnOs(OS.WINDOWS)
class BackupCommandRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void backup_whenPowerShellWritesGbkAndExitsNonZero_reportsExitCode() throws Exception {
        Path script = writeScript("gbk-error.ps1", """
                $bytes = [byte[]](0xB2,0xE2,0xCA,0xD4)
                [Console]::OpenStandardOutput().Write($bytes, 0, $bytes.Length)
                exit 7
                """);
        BackupCommandRunner runner = runner(script, Duration.ofSeconds(5));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> runner.backup(tempDir));

        assertEquals("备份任务执行失败，退出码: 7", error.getMessage());
    }

    @Test
    void backup_whenPowerShellExceedsTimeout_terminatesAndReportsTimeout() throws Exception {
        Path script = writeScript("timeout.ps1", "Start-Sleep -Seconds 5\nexit 0\n");
        BackupCommandRunner runner = runner(script, Duration.ofSeconds(1));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> runner.backup(tempDir));

        assertEquals("备份任务执行超时", error.getMessage());
    }

    private BackupCommandRunner runner(Path script, Duration timeout) {
        BackupProperties properties = new BackupProperties();
        properties.setCommandTimeout(timeout);
        BackupRuntime runtime = new BackupRuntime("WINDOWS", "POWERSHELL", tempDir,
                script, script, tempDir.resolve("missing.env"), List.of());
        BackupRuntimeResolver resolver = mock(BackupRuntimeResolver.class);
        BackupProcessEnvironment environment = mock(BackupProcessEnvironment.class);
        when(resolver.resolve()).thenReturn(runtime);
        when(environment.variables()).thenReturn(Map.of());
        return new BackupCommandRunner(properties, resolver, environment);
    }

    private Path writeScript(String name, String content) throws Exception {
        Path script = tempDir.resolve(name);
        Files.writeString(script, content, StandardCharsets.US_ASCII);
        return script;
    }
}
