package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportTaskMaintenanceService {
    private static final int STATUS_QUEUED = 1;
    private static final int STATUS_RUNNING = 2;
    private static final int STATUS_SUCCESS = 3;
    private static final int STATUS_FAILED = 4;
    private static final int STATUS_EXPIRED = 6;

    private final ExportTaskMapper taskMapper;
    private final ExportTaskStorage storage;
    private final ExportTaskExecutor executor;

    public int recoverStaleRunning(LocalDateTime cutoff) {
        return taskMapper.update(null, new LambdaUpdateWrapper<ExportTask>()
                .eq(ExportTask::getTaskStatus, STATUS_RUNNING)
                .and(item -> item.lt(ExportTask::getHeartbeatAt, cutoff)
                        .or().isNull(ExportTask::getHeartbeatAt).lt(ExportTask::getStartedAt, cutoff))
                .set(ExportTask::getTaskStatus, STATUS_QUEUED).set(ExportTask::getProgress, 0)
                .set(ExportTask::getWorkerId, null).set(ExportTask::getHeartbeatAt, null)
                .setSql("version = version + 1"));
    }

    public int failExhaustedQueued() {
        return taskMapper.update(null, new LambdaUpdateWrapper<ExportTask>()
                .eq(ExportTask::getTaskStatus, STATUS_QUEUED)
                .apply("attempt_count >= max_attempts")
                .set(ExportTask::getTaskStatus, STATUS_FAILED).set(ExportTask::getProgress, 100)
                .set(ExportTask::getErrorMessage, "导出任务已达到最大重试次数")
                .set(ExportTask::getCompletedAt, LocalDateTime.now())
                .setSql("version = version + 1"));
    }

    public int expireCompleted(LocalDateTime now) {
        List<ExportTask> expired = taskMapper.selectList(new LambdaQueryWrapper<ExportTask>()
                .eq(ExportTask::getTaskStatus, STATUS_SUCCESS).le(ExportTask::getExpiresAt, now)
                .orderByAsc(ExportTask::getExpiresAt).last("LIMIT 100"));
        int updated = 0;
        for (ExportTask task : expired) {
            storage.delete(task);
            updated += markExpired(task.getUuid());
        }
        return updated;
    }

    public int dispatchPending() {
        List<ExportTask> pending = taskMapper.selectList(new LambdaQueryWrapper<ExportTask>()
                .eq(ExportTask::getTaskStatus, STATUS_QUEUED).apply("attempt_count < max_attempts")
                .orderByAsc(ExportTask::getCreateTime).last("LIMIT 20"));
        return pending.stream().mapToInt(task -> executor.submit(task.getUuid()) ? 1 : 0).sum();
    }

    public int cleanupOrphanArtifacts(LocalDateTime cutoff, int limit) {
        List<Path> candidates = storage.listExecutionArtifactsBefore(cutoff, limit);
        if (candidates.isEmpty()) return 0;
        Set<String> referencedPaths = referencedPaths(candidates);
        Set<String> activePrefixes = activeExecutionPrefixes();
        return (int) candidates.stream()
                .filter(path -> !isProtected(path, referencedPaths, activePrefixes))
                .filter(storage::deleteIfExists)
                .count();
    }

    private Set<String> referencedPaths(List<Path> candidates) {
        List<String> paths = candidates.stream()
                .flatMap(path -> java.util.stream.Stream.of(path.toString(), storage.storageKey(path)))
                .distinct().toList();
        List<ExportTask> referenced = taskMapper.selectList(new LambdaQueryWrapper<ExportTask>()
                .select(ExportTask::getFilePath).in(ExportTask::getFilePath, paths));
        return referenced.stream().map(ExportTask::getFilePath).collect(Collectors.toSet());
    }

    private Set<String> activeExecutionPrefixes() {
        List<ExportTask> active = taskMapper.selectList(new LambdaQueryWrapper<ExportTask>()
                .select(ExportTask::getUuid, ExportTask::getWorkerId)
                .eq(ExportTask::getTaskStatus, STATUS_RUNNING).isNotNull(ExportTask::getWorkerId));
        return active.stream().map(this::activePrefix).flatMap(Optional::stream).collect(Collectors.toSet());
    }

    private Optional<String> activePrefix(ExportTask task) {
        try {
            return Optional.of(storage.executionArtifactPrefix(task.getUuid(), task.getWorkerId()));
        } catch (BusinessException exception) {
            return Optional.empty();
        }
    }

    private boolean isProtected(Path path, Set<String> referencedPaths, Set<String> activePrefixes) {
        if (referencedPaths.contains(path.toString()) || referencedPaths.contains(storage.storageKey(path))) {
            return true;
        }
        String fileName = path.getFileName().toString();
        return activePrefixes.stream().anyMatch(fileName::startsWith);
    }

    private int markExpired(String uuid) {
        return taskMapper.update(null, new LambdaUpdateWrapper<ExportTask>()
                .eq(ExportTask::getUuid, uuid).eq(ExportTask::getTaskStatus, STATUS_SUCCESS)
                .set(ExportTask::getTaskStatus, STATUS_EXPIRED).set(ExportTask::getFilePath, null)
                .setSql("version = version + 1"));
    }
}
