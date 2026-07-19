package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.exporttask.config.ExportTaskRuntimeProperties;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ExportTaskExecutionLeaseService {
    private static final int STATUS_QUEUED = 1;
    private static final int STATUS_RUNNING = 2;
    private static final int STATUS_SUCCESS = 3;
    private static final int STATUS_FAILED = 4;

    private final ExportTaskMapper taskMapper;
    private final long heartbeatIntervalSeconds;
    private final ScheduledThreadPoolExecutor heartbeatExecutor;

    public ExportTaskExecutionLeaseService(ExportTaskMapper taskMapper, ExportTaskRuntimeProperties properties) {
        this.taskMapper = taskMapper;
        this.heartbeatIntervalSeconds = properties.getHeartbeatIntervalSeconds();
        this.heartbeatExecutor = new ScheduledThreadPoolExecutor(1,
                Thread.ofPlatform().name("export-task-heartbeat-", 1).factory());
        heartbeatExecutor.setRemoveOnCancelPolicy(true);
    }

    public Optional<ExportTaskExecutionLease> claim(String taskUuid) {
        ExportTask task = taskMapper.selectById(taskUuid);
        if (task == null || !Integer.valueOf(STATUS_QUEUED).equals(task.getTaskStatus())) return Optional.empty();
        ExportTaskExecutionLease lease = new ExportTaskExecutionLease(task, UUID.randomUUID().toString());
        return markRunning(lease) ? Optional.of(lease) : Optional.empty();
    }

    public ExportTaskHeartbeat startHeartbeat(ExportTaskExecutionLease lease) {
        AtomicBoolean active = new AtomicBoolean(true);
        ScheduledFuture<?> future = heartbeatExecutor.scheduleAtFixedRate(
                () -> renewSafely(lease, active), heartbeatIntervalSeconds,
                heartbeatIntervalSeconds, TimeUnit.SECONDS);
        return () -> {
            active.set(false);
            future.cancel(false);
        };
    }

    public boolean renew(ExportTaskExecutionLease lease) {
        return taskMapper.update(null, ownedRunningUpdate(lease)
                .set(ExportTask::getHeartbeatAt, LocalDateTime.now())) > 0;
    }

    public boolean markSuccess(ExportTaskExecutionLease lease, Path path,
                               ExportTaskArtifact artifact) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        return taskMapper.update(null, ownedRunningUpdate(lease)
                .set(ExportTask::getTaskStatus, STATUS_SUCCESS).set(ExportTask::getProgress, 100)
                .set(ExportTask::getFileName, artifact.fileName())
                .set(ExportTask::getContentType, artifact.contentType())
                // Store only the execution filename; the configured storage root is instance-local.
                .set(ExportTask::getFilePath, path.getFileName().toString())
                .set(ExportTask::getFileSize, Files.size(path))
                .set(ExportTask::getCompletedAt, now).set(ExportTask::getHeartbeatAt, now)
                .setSql("version = version + 1")) > 0;
    }

    public boolean markFailure(ExportTaskExecutionLease lease, String message) {
        return taskMapper.update(null, ownedRunningUpdate(lease)
                .set(ExportTask::getTaskStatus, STATUS_FAILED).set(ExportTask::getProgress, 100)
                .set(ExportTask::getErrorMessage, message).set(ExportTask::getCompletedAt, LocalDateTime.now())
                .setSql("version = version + 1")) > 0;
    }

    private boolean markRunning(ExportTaskExecutionLease lease) {
        LocalDateTime now = LocalDateTime.now();
        return taskMapper.update(null, new LambdaUpdateWrapper<ExportTask>()
                .eq(ExportTask::getUuid, lease.task().getUuid())
                .eq(ExportTask::getTaskStatus, STATUS_QUEUED)
                .set(ExportTask::getTaskStatus, STATUS_RUNNING).set(ExportTask::getProgress, 10)
                .set(ExportTask::getStartedAt, now).set(ExportTask::getHeartbeatAt, now)
                .set(ExportTask::getWorkerId, lease.token())
                .setSql("attempt_count = attempt_count + 1, version = version + 1")) > 0;
    }

    private LambdaUpdateWrapper<ExportTask> ownedRunningUpdate(ExportTaskExecutionLease lease) {
        return new LambdaUpdateWrapper<ExportTask>()
                .eq(ExportTask::getUuid, lease.task().getUuid())
                .eq(ExportTask::getTaskStatus, STATUS_RUNNING)
                .eq(ExportTask::getWorkerId, lease.token());
    }

    private void renewSafely(ExportTaskExecutionLease lease, AtomicBoolean active) {
        if (!active.get()) return;
        try {
            if (!renew(lease)) active.set(false);
        } catch (RuntimeException exception) {
            log.warn("Export task heartbeat failed temporarily: {}", lease.task().getUuid(), exception);
        }
    }

    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdownNow();
    }
}
