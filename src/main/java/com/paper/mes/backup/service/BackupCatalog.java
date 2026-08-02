package com.paper.mes.backup.service;

import com.paper.mes.backup.dto.BackupRecordVO;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
@Slf4j
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

    public Path requireVerifiableBackup(String backupId) {
        Path directory = requireBackup(backupId);
        if (!"COMPLETE".equals(toRecord(directory).getIntegrityStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "备份不完整，无法进行隔离恢复验证");
        }
        return directory;
    }

    public Path root() {
        return runtimeResolver.resolve().root();
    }

    private BackupRecordVO toRecord(Path directory) {
        String id = directory.getFileName().toString();
        Path database = directory.resolve(properties.getSourceDbName() + ".sql.gz");
        Path checksum = directory.resolve("SHA256SUMS");
        Path report = directory.resolve("restore-check.txt");
        RequiredFile databaseFile = inspectRequiredFile(database);
        RequiredFile checksumFile = inspectRequiredFile(checksum);
        DirectorySize size = directorySize(directory);
        List<String> missingItems = missingItems(databaseFile, checksumFile);
        boolean readable = size.readable() && databaseFile.readable() && checksumFile.readable();
        LocalDateTime verifiedAt = readVerifiedAt(report);
        return BackupRecordVO.builder()
                .id(id)
                .createdAt(parseBackupTime(id))
                .sizeBytes(size.bytes())
                .databaseArchive(databaseFile.usable())
                .uploadIncluded(Files.isRegularFile(directory.resolve("upload.tar.gz")))
                .checksumAvailable(checksumFile.usable())
                .integrityStatus(integrityStatus(readable, missingItems))
                .missingItems(missingItems)
                .verificationStatus(verifiedAt == null ? "UNVERIFIED" : "VERIFIED")
                .verifiedAt(verifiedAt)
                .build();
    }

    private DirectorySize directorySize(Path directory) {
        try (Stream<Path> files = Files.walk(directory)) {
            List<FileSize> sizes = files.filter(Files::isRegularFile).map(this::fileSize).toList();
            long bytes = sizes.stream().mapToLong(FileSize::bytes).sum();
            boolean readable = sizes.stream().allMatch(FileSize::readable);
            return new DirectorySize(bytes, readable);
        } catch (IOException ex) {
            log.warn("Unable to inspect backup directory size: {}", directory, ex);
            return new DirectorySize(0L, false);
        }
    }

    private FileSize fileSize(Path path) {
        try {
            return new FileSize(Files.size(path), true);
        } catch (IOException ex) {
            log.warn("Unable to read backup file size: {}", path, ex);
            return new FileSize(0L, false);
        }
    }

    private RequiredFile inspectRequiredFile(Path path) {
        if (!Files.isRegularFile(path)) return new RequiredFile(false, true);
        try {
            return new RequiredFile(Files.size(path) > 0, true);
        } catch (IOException ex) {
            log.warn("Unable to inspect required backup file: {}", path, ex);
            return new RequiredFile(false, false);
        }
    }

    private List<String> missingItems(RequiredFile database, RequiredFile checksum) {
        List<String> missing = new ArrayList<>(2);
        if (database.readable() && !database.usable()) missing.add("数据库文件");
        if (checksum.readable() && !checksum.usable()) missing.add("校验文件");
        return List.copyOf(missing);
    }

    private String integrityStatus(boolean readable, List<String> missingItems) {
        if (!readable) return "REVIEW";
        return missingItems.isEmpty() ? "COMPLETE" : "INCOMPLETE";
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

    private record DirectorySize(long bytes, boolean readable) {
    }

    private record FileSize(long bytes, boolean readable) {
    }

    private record RequiredFile(boolean usable, boolean readable) {
    }
}
