package com.paper.mes.backup.service;

import com.paper.mes.backup.config.BackupProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class BackupRuntimeResolver {

    private final BackupProperties properties;
    private volatile BackupRuntime cached;

    public BackupRuntimeResolver(BackupProperties properties) {
        this.properties = properties;
    }

    public BackupRuntime resolve() {
        BackupRuntime current = cached;
        if (current != null) return current;
        synchronized (this) {
            if (cached == null) cached = detect();
            return cached;
        }
    }

    private BackupRuntime detect() {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path backupScript = resolveScript(properties.getBackupScript(), backupCandidates(windows));
        Path verifyScript = resolveScript(properties.getVerifyScript(), verifyCandidates(windows));
        List<String> missing = missingComponents(windows, backupScript, verifyScript);
        return new BackupRuntime(
                windows ? "WINDOWS" : "LINUX",
                windows ? "POWERSHELL" : "BASH",
                resolveRoot(windows),
                backupScript,
                verifyScript,
                resolveEnvFile(windows),
                missing);
    }

    private Path resolveRoot(boolean windows) {
        if (StringUtils.hasText(properties.getRootDir())) {
            return normalized(properties.getRootDir());
        }
        return windows ? normalized(Path.of(System.getProperty("user.dir"), "backup").toString())
                : Path.of("/opt/backups/paper-mes");
    }

    private Path resolveEnvFile(boolean windows) {
        if (StringUtils.hasText(properties.getEnvFile())) {
            return normalized(properties.getEnvFile());
        }
        return windows ? normalized(Path.of(System.getProperty("user.dir"), "deploy", "backup.local.env").toString())
                : Path.of("/etc/paper-mes/backup.env");
    }

    private Path resolveScript(String override, List<Path> candidates) {
        if (StringUtils.hasText(override)) {
            return normalized(override);
        }
        return candidates.stream().filter(Files::isRegularFile).findFirst().orElse(candidates.getFirst());
    }

    private List<Path> backupCandidates(boolean windows) {
        String file = windows ? "backup-paper-mes.example.ps1" : "backup-paper-mes.example.sh";
        return scriptCandidates(file, windows ? null : "/usr/local/bin/backup-paper-mes");
    }

    private List<Path> verifyCandidates(boolean windows) {
        String file = windows ? "verify-backup-restore.example.ps1" : "verify-backup-restore.example.sh";
        return scriptCandidates(file, windows ? null : "/usr/local/bin/verify-paper-mes-backup");
    }

    private List<Path> scriptCandidates(String repositoryFile, String installedFile) {
        List<Path> candidates = new ArrayList<>();
        candidates.add(normalized(Path.of(System.getProperty("user.dir"), "deploy", repositoryFile).toString()));
        candidates.add(Path.of("/opt/paper-mes/source/deploy", repositoryFile));
        if (installedFile != null) candidates.add(Path.of(installedFile));
        return candidates;
    }

    private List<String> missingComponents(boolean windows, Path backupScript, Path verifyScript) {
        List<String> missing = new ArrayList<>();
        if (!Files.isRegularFile(backupScript)) missing.add("backup-script");
        if (!Files.isRegularFile(verifyScript)) missing.add("verify-script");
        if (windows && !commandExists("powershell.exe")) missing.add("powershell");
        if (!windows && !commandExists("bash")) missing.add("bash");
        for (String command : windows
                ? List.of("mysqldump.exe", "mysql.exe", "tar.exe")
                : List.of("mysqldump", "mysql", "gzip", "tar", "sha256sum", "flock", "realpath")) {
            if (!commandExists(command)) missing.add(command.replace(".exe", ""));
        }
        return List.copyOf(missing);
    }

    private boolean commandExists(String command) {
        try {
            List<String> probe = command.startsWith("powershell")
                    ? List.of(command, "-NoProfile", "-NonInteractive", "-Command", "exit 0")
                    : List.of(command, "--version");
            Process process = new ProcessBuilder(probe)
                    .redirectErrorStream(true).start();
            return process.waitFor() == 0;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private Path normalized(String value) {
        return Path.of(value).toAbsolutePath().normalize();
    }
}
