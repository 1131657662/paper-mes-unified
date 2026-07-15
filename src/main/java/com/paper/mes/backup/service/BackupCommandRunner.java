package com.paper.mes.backup.service;

import com.paper.mes.backup.config.BackupProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class BackupCommandRunner {

    private static final int MAX_LOG_LENGTH = 8000;
    private final BackupProperties properties;
    private final BackupRuntimeResolver runtimeResolver;
    private final BackupProcessEnvironment processEnvironment;

    public BackupCommandRunner(BackupProperties properties, BackupRuntimeResolver runtimeResolver,
                               BackupProcessEnvironment processEnvironment) {
        this.properties = properties;
        this.runtimeResolver = runtimeResolver;
        this.processEnvironment = processEnvironment;
    }

    public void backup(Path root) {
        run(runtimeResolver.resolve().backupScript(), Map.of("BACKUP_ROOT", root.toString()));
    }

    public void verify(Path root, Path backupDirectory) {
        run(runtimeResolver.resolve().verifyScript(), Map.of(
                "BACKUP_ROOT", root.toString(),
                "BACKUP_DIR", backupDirectory.toString()));
    }

    private void run(Path script, Map<String, String> variables) {
        BackupRuntime runtime = runtimeResolver.resolve();
        Path outputFile = createOutputFile();
        try {
            Process process;
            try {
                process = startProcess(runtime, script, variables, outputFile);
            } catch (IOException ex) {
                throw new IllegalStateException("无法启动备份脚本", ex);
            }
            waitFor(process);
            String output = readOutput(outputFile, runtime);
            log.info("Backup command output: {}", truncate(output));
            if (process.exitValue() != 0) {
                throw new IllegalStateException("备份任务执行失败，退出码: " + process.exitValue());
            }
        } finally {
            deleteOutputFile(outputFile);
        }
    }

    private String readOutput(Path outputFile, BackupRuntime runtime) {
        Charset charset = "WINDOWS".equals(runtime.platform())
                ? Charset.forName("GB18030") : StandardCharsets.UTF_8;
        try {
            return new String(Files.readAllBytes(outputFile), charset);
        } catch (IOException ex) {
            throw new IllegalStateException("无法读取备份任务日志", ex);
        }
    }

    private Process startProcess(BackupRuntime runtime, Path script, Map<String, String> variables,
                                 Path outputFile) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command(runtime, script))
                .redirectErrorStream(true).redirectOutput(outputFile.toFile());
        builder.environment().putAll(processVariables(runtime));
        builder.environment().put("BACKUP_ENV_FILE", runtime.envFile().toString());
        builder.environment().putAll(variables);
        return builder.start();
    }

    private Map<String, String> processVariables(BackupRuntime runtime) {
        Map<String, String> variables = new java.util.HashMap<>(processEnvironment.variables());
        if (Files.isRegularFile(runtime.envFile())) {
            variables.remove("DB_PASSWORD");
            variables.remove("DB_ADMIN_PASSWORD");
        }
        return variables;
    }

    private java.util.List<String> command(BackupRuntime runtime, Path script) {
        if ("WINDOWS".equals(runtime.platform())) {
            return java.util.List.of("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-File", script.toString());
        }
        return java.util.List.of("bash", script.toString());
    }

    private void waitFor(Process process) {
        try {
            boolean finished = process.waitFor(properties.getCommandTimeout().toSeconds(), TimeUnit.SECONDS);
            if (finished) return;
            terminateProcess(process);
            throw new IllegalStateException("备份任务执行超时");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("备份任务被中断", ex);
        }
    }

    private void terminateProcess(Process process) throws InterruptedException {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            log.warn("Backup process did not terminate within 5 seconds after timeout");
        }
    }

    private Path createOutputFile() {
        try {
            return Files.createTempFile("paper-mes-backup-", ".log");
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建备份任务日志", ex);
        }
    }

    private void deleteOutputFile(Path outputFile) {
        IOException failure = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                Files.deleteIfExists(outputFile);
                return;
            } catch (IOException ex) {
                failure = ex;
                if (!pauseBeforeDeleteRetry()) break;
            }
        }
        log.warn("Failed to delete backup command output: {}", outputFile, failure);
    }

    private boolean pauseBeforeDeleteRetry() {
        try {
            Thread.sleep(100);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String truncate(String output) {
        return output.length() <= MAX_LOG_LENGTH ? output : output.substring(0, MAX_LOG_LENGTH);
    }
}
