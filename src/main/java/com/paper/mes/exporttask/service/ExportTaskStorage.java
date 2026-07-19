package com.paper.mes.exporttask.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.exporttask.entity.ExportTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Component
public class ExportTaskStorage {
    private static final String EXECUTION_FILE_PREFIX = "export-task-";
    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9-]{1,100}");
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[a-z0-9]{1,10}");
    private static final Pattern EXECUTION_FILE_NAME = Pattern.compile(
            "export-task-[A-Za-z0-9-]{1,100}-[A-Za-z0-9-]{1,100}\\.[a-z0-9]{1,10}");

    private final Path root;

    @Value("${app.export-task.storage-min-free-bytes:0}")
    private long minimumFreeBytes;
    @Value("${app.export-task.storage-min-free-percent:5}")
    private double minimumFreePercent;

    public ExportTaskStorage(@Value("${app.export-task.storage-dir:./data/exports}") String storageDir) {
        root = Path.of(storageDir).toAbsolutePath().normalize();
    }

    public Path target(ExportTaskExecutionLease lease, String extension) {
        if (lease == null || lease.task() == null) {
            throw new BusinessException("导出任务执行租约不合法");
        }
        if (extension == null || !SAFE_EXTENSION.matcher(extension).matches()) {
            throw new BusinessException("导出文件扩展名不合法");
        }
        ensureRoot();
        assertReadyForWrite();
        String fileName = executionArtifactPrefix(lease.task().getUuid(), lease.token()) + extension;
        return root.resolve(fileName).normalize();
    }

    /** Persist a root-relative artifact key for shared storage across instances. */
    public String storageKey(Path path) {
        Path normalized = requireInsideRoot(path);
        return root.relativize(normalized).toString().replace('\\', '/');
    }

    public ExportTaskStorageHealth health(long minFreeBytes, double minFreePercent) {
        LocalDateTime checkedAt = LocalDateTime.now();
        try {
            ensureRoot();
            if (!Files.isDirectory(root)) {
                return new ExportTaskStorageHealth(ExportTaskStorageHealth.UNAVAILABLE,
                        false, false, 0, 0, 0, checkedAt);
            }
            boolean writable = Files.isWritable(root);
            FileStoreUsage usage = fileStoreUsage();
            if (!writable) return usage.health(ExportTaskStorageHealth.READ_ONLY, false, false, checkedAt);
            if (usage.freeBytes() < minFreeBytes || usage.freePercent() < minFreePercent) {
                return usage.health(ExportTaskStorageHealth.LOW_SPACE, false, true, checkedAt);
            }
            return usage.health(ExportTaskStorageHealth.READY, true, true, checkedAt);
        } catch (IOException | RuntimeException exception) {
            log.warn("Export task storage health check failed", exception);
            return new ExportTaskStorageHealth(ExportTaskStorageHealth.ERROR,
                    false, false, 0, 0, 0, checkedAt);
        }
    }

    public void assertReadyForWrite() {
        ExportTaskStorageHealth health = health(minimumFreeBytes, minimumFreePercent);
        if (!health.available()) throw new BusinessException("Export storage is temporarily unavailable");
    }

    public String executionArtifactPrefix(String taskUuid, String token) {
        validateSegment(taskUuid, "导出任务标识不合法");
        validateSegment(token, "导出执行标识不合法");
        return EXECUTION_FILE_PREFIX + taskUuid + "-" + token + ".";
    }

    public List<Path> listExecutionArtifactsBefore(LocalDateTime cutoff, int limit) {
        if (cutoff == null || limit <= 0 || !Files.isDirectory(root)) return List.of();
        Instant cutoffInstant = cutoff.atZone(ZoneId.systemDefault()).toInstant();
        try (Stream<Path> files = Files.list(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(EXECUTION_FILE_PREFIX))
                    .filter(path -> isBefore(path, cutoffInstant))
                    .sorted(Comparator.comparing(this::lastModified).thenComparing(Path::toString))
                    .limit(limit).toList();
        } catch (IOException exception) {
            log.warn("Unable to scan export task artifacts", exception);
            return List.of();
        }
    }

    private void ensureRoot() {
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new BusinessException("导出任务存储目录不可用");
        }
    }

    private FileStoreUsage fileStoreUsage() throws IOException {
        var fileStore = Files.getFileStore(root);
        long total = fileStore.getTotalSpace();
        long free = fileStore.getUsableSpace();
        double percent = total <= 0 ? 0 : free * 100D / total;
        return new FileStoreUsage(free, total, percent);
    }

    private record FileStoreUsage(long freeBytes, long totalBytes, double freePercent) {
        private ExportTaskStorageHealth health(String status, boolean available,
                                               boolean writable, LocalDateTime checkedAt) {
            return new ExportTaskStorageHealth(status, available, writable,
                    freeBytes, totalBytes, freePercent, checkedAt);
        }
    }

    private void validateSegment(String value, String message) {
        if (value == null || !SAFE_SEGMENT.matcher(value).matches()) throw new BusinessException(message);
    }

    public Path requireFile(ExportTask task) {
        if (task == null || task.getFilePath() == null || task.getFilePath().isBlank()) {
            throw new BusinessException("导出文件尚未生成");
        }
        Path path = resolveStoredPath(task.getFilePath());
        if (path == null || !Files.isRegularFile(path)) {
            throw new BusinessException("导出文件不存在或已过期");
        }
        return path;
    }

    public void delete(ExportTask task) {
        if (task == null || task.getFilePath() == null || task.getFilePath().isBlank()) return;
        Path path = resolveStoredPath(task.getFilePath());
        if (path != null) delete(path);
    }

    public void delete(Path path) {
        deleteIfExists(path);
    }

    public boolean deleteIfExists(Path path) {
        if (path == null) return false;
        path = path.toAbsolutePath().normalize();
        if (!path.startsWith(root)) return false;
        try {
            return Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Expiry cleanup is best effort and will retry on the next maintenance cycle.
            return false;
        }
    }

    private Path resolveStoredPath(String storedPath) {
        Path raw;
        try {
            raw = Path.of(storedPath);
        } catch (RuntimeException exception) {
            return null;
        }
        if (!raw.isAbsolute()) return resolveInsideRoot(raw);
        Path normalized = raw.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) return normalized;
        Path fileName = normalized.getFileName();
        String name = fileName == null ? "" : fileName.toString();
        return EXECUTION_FILE_NAME.matcher(name).matches() ? resolveInsideRoot(fileName) : null;
    }

    private Path resolveInsideRoot(Path relative) {
        Path resolved = root.resolve(relative).normalize();
        return resolved.startsWith(root) ? resolved : null;
    }

    private Path requireInsideRoot(Path path) {
        if (path == null) throw new BusinessException("Invalid export artifact path");
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) throw new BusinessException("Invalid export artifact path");
        return normalized;
    }

    private boolean isBefore(Path path, Instant cutoff) {
        return lastModified(path).isBefore(cutoff);
    }

    private Instant lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException exception) {
            return Instant.MAX;
        }
    }
}
