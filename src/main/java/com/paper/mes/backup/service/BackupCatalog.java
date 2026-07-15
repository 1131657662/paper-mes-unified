package com.paper.mes.backup.service;

import com.paper.mes.backup.dto.BackupRecordVO;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class BackupCatalog {

    private static final Pattern BACKUP_ID = Pattern.compile("\\d{8}-\\d{6}");
    private static final DateTimeFormatter ID_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter REPORT_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final BackupRuntimeResolver runtimeResolver;
    private final com.paper.mes.backup.config.BackupProperties properties;

    public BackupCatalog(com.paper.mes.backup.config.BackupProperties properties,
                         BackupRuntimeResolver runtimeResolver) {
        this.properties = properties;
        this.runtimeResolver = runtimeResolver;
    }

    public List<BackupRecordVO> list() {
        Path root = root();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(root)) {
            return entries.filter(Files::isDirectory)
                    .filter(path -> !Files.isSymbolicLink(path))
                    .filter(path -> BACKUP_ID.matcher(path.getFileName().toString()).matches())
                    .map(this::toRecord)
                    .sorted(Comparator.comparing(BackupRecordVO::getCreatedAt).reversed())
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("无法读取备份目录", ex);
        }
    }

    public Path requireBackup(String backupId) {
        if (!BACKUP_ID.matcher(backupId).matches()) {
            throw new IllegalArgumentException("备份编号格式不正确");
        }
        Path rootReal = realPath(root(), "备份根目录不存在");
        Path candidate = rootReal.resolve(backupId).normalize();
        if (!candidate.startsWith(rootReal) || !Files.isDirectory(candidate)) {
            throw new IllegalArgumentException("备份记录不存在");
        }
        Path candidateReal = realPath(candidate, "备份记录不存在");
        if (!candidateReal.startsWith(rootReal)) {
            throw new IllegalArgumentException("备份记录路径不安全");
        }
        return candidateReal;
    }

    public Path root() {
        return runtimeResolver.resolve().root();
    }

    private BackupRecordVO toRecord(Path directory) {
        String id = directory.getFileName().toString();
        Path database = directory.resolve(properties.getSourceDbName() + ".sql.gz");
        Path report = directory.resolve("restore-check.txt");
        return BackupRecordVO.builder()
                .id(id)
                .createdAt(parseBackupTime(id))
                .sizeBytes(directorySize(directory))
                .databaseArchive(Files.isRegularFile(database))
                .uploadIncluded(Files.isRegularFile(directory.resolve("upload.tar.gz")))
                .checksumAvailable(Files.isRegularFile(directory.resolve("SHA256SUMS")))
                .verificationStatus(Files.isRegularFile(report) ? "VERIFIED" : "UNVERIFIED")
                .verifiedAt(readVerifiedAt(report))
                .build();
    }

    private long directorySize(Path directory) {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).mapToLong(this::fileSize).sum();
        } catch (IOException ex) {
            return 0L;
        }
    }

    private long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            return 0L;
        }
    }

    private LocalDateTime readVerifiedAt(Path report) {
        if (!Files.isRegularFile(report)) return null;
        try (Stream<String> lines = Files.lines(report)) {
            return lines.filter(line -> line.startsWith("verified_at="))
                    .map(line -> line.substring("verified_at=".length()))
                    .map(value -> REPORT_TIME.parse(value, LocalDateTime::from))
                    .findFirst().orElse(null);
        } catch (IOException | DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDateTime parseBackupTime(String backupId) {
        return LocalDateTime.parse(backupId, ID_FORMAT);
    }

    private Path realPath(Path path, String message) {
        try {
            return path.toRealPath();
        } catch (IOException ex) {
            throw new IllegalArgumentException(message);
        }
    }
}
