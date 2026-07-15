package com.paper.mes.backup.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.regex.Pattern;

@Slf4j
@Component
public class OffsiteBackupStatusReader {

    private static final String STATUS_FILE = ".remote-sync-status";
    private static final long MAX_FILE_SIZE = 4096;
    private static final Pattern REMOTE_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private final BackupCatalog catalog;

    public OffsiteBackupStatusReader(BackupCatalog catalog) {
        this.catalog = catalog;
    }

    public OffsiteBackupStatus read() {
        Path statusFile = catalog.root().resolve(STATUS_FILE).normalize();
        try {
            if (!Files.exists(statusFile, LinkOption.NOFOLLOW_LINKS)) {
                return OffsiteBackupStatus.notConfigured();
            }
            if (!isReadableStatusFile(statusFile)) return OffsiteBackupStatus.invalid();
            return parse(statusFile);
        } catch (IOException | IllegalArgumentException | SecurityException ex) {
            log.warn("Invalid offsite backup status file: {}", statusFile, ex);
            return OffsiteBackupStatus.invalid();
        }
    }

    private boolean isReadableStatusFile(Path statusFile) {
        return statusFile.startsWith(catalog.root())
                && Files.isRegularFile(statusFile, LinkOption.NOFOLLOW_LINKS)
                && fileSize(statusFile) <= MAX_FILE_SIZE;
    }

    private OffsiteBackupStatus parse(Path statusFile) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(statusFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        if (!"1".equals(properties.getProperty("version"))) {
            throw new IllegalArgumentException("Unsupported status version");
        }
        OffsiteBackupStatus.State state = parseState(properties.getProperty("status"));
        String remoteName = requireRemoteName(properties.getProperty("remote_name"));
        return new OffsiteBackupStatus(state, parseTime(properties.getProperty("completed_at")), remoteName);
    }

    private OffsiteBackupStatus.State parseState(String value) {
        if ("SUCCESS".equals(value)) return OffsiteBackupStatus.State.SUCCESS;
        if ("FAILED".equals(value)) return OffsiteBackupStatus.State.FAILED;
        throw new IllegalArgumentException("Unsupported sync status");
    }

    private String requireRemoteName(String value) {
        if (value == null || !REMOTE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid remote name");
        }
        return value;
    }

    private java.time.LocalDateTime parseTime(String value) {
        try {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.ofHours(8)).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid sync completion time", ex);
        }
    }

    private long fileSize(Path statusFile) {
        try {
            return Files.size(statusFile);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read status size", ex);
        }
    }
}
