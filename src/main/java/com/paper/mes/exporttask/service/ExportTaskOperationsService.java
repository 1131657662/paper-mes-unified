package com.paper.mes.exporttask.service;

import com.paper.mes.exporttask.config.ExportTaskRuntimeProperties;
import com.paper.mes.exporttask.dto.ExportTaskOperationsIssueVO;
import com.paper.mes.exporttask.dto.ExportTaskOperationsIssuesVO;
import com.paper.mes.exporttask.dto.ExportTaskOperationsVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
public class ExportTaskOperationsService {
    private final JdbcTemplate jdbcTemplate;
    private final ExportTaskEventPublisher eventPublisher;
    private final ExportTaskExecutor taskExecutor;
    private final ExportTaskStorage storage;
    private final long staleMinutes;
    private final long storageMinFreeBytes;
    private final double storageMinFreePercent;

    public ExportTaskOperationsService(
            JdbcTemplate jdbcTemplate,
            ExportTaskEventPublisher eventPublisher,
            ExportTaskExecutor taskExecutor,
            ExportTaskRuntimeProperties runtimeProperties,
            ExportTaskStorage storage) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventPublisher = eventPublisher;
        this.taskExecutor = taskExecutor;
        this.storage = storage;
        this.staleMinutes = runtimeProperties.getStaleMinutes();
        this.storageMinFreeBytes = runtimeProperties.getStorageMinFreeBytes();
        this.storageMinFreePercent = runtimeProperties.getStorageMinFreePercent();
    }

    public ExportTaskOperationsVO overview() {
        LocalDateTime asOf = LocalDateTime.now();
        LocalDateTime staleCutoff = asOf.minusMinutes(staleMinutes);
        ExportTaskExecutorSnapshot executorSnapshot = taskExecutor.snapshot();
        ExportTaskStorageHealth storageHealth = storage.health(storageMinFreeBytes, storageMinFreePercent);
        return jdbcTemplate.queryForObject("""
                SELECT
                  SUM(CASE WHEN task_status = 1 THEN 1 ELSE 0 END) AS queued_count,
                  SUM(CASE WHEN task_status = 2 THEN 1 ELSE 0 END) AS running_count,
                  SUM(CASE WHEN task_status = 3 AND completed_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
                           THEN 1 ELSE 0 END) AS succeeded_24h,
                  SUM(CASE WHEN task_status = 4 AND completed_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
                           THEN 1 ELSE 0 END) AS failed_24h,
                  SUM(CASE WHEN task_status = 2 AND COALESCE(heartbeat_at, started_at) < ?
                           THEN 1 ELSE 0 END) AS stale_running_count,
                  MIN(CASE WHEN task_status = 1 THEN create_time END) AS oldest_queued_at,
                  COALESCE(AVG(CASE WHEN started_at IS NOT NULL AND completed_at IS NOT NULL
                           THEN TIMESTAMPDIFF(SECOND, started_at, completed_at) END), 0) AS avg_duration_seconds,
                  COALESCE(SUM(CASE WHEN task_status = 3 THEN file_size ELSE 0 END), 0) AS stored_file_bytes
                FROM sys_export_task WHERE is_deleted = 0
                """, (resultSet, rowNum) -> new ExportTaskOperationsVO(
                resultSet.getLong("queued_count"), resultSet.getLong("running_count"),
                resultSet.getLong("succeeded_24h"), resultSet.getLong("failed_24h"),
                resultSet.getLong("stale_running_count"), localDateTime(resultSet.getTimestamp("oldest_queued_at")),
                resultSet.getDouble("avg_duration_seconds"), resultSet.getLong("stored_file_bytes"),
                eventPublisher.connectionCount(), executorSnapshot.workerCount(),
                executorSnapshot.activeWorkerCount(), executorSnapshot.queuedInMemoryCount(),
                executorSnapshot.queueCapacity(), executorSnapshot.rejectedSubmissionCount(),
                executorSnapshot.completedExecutionCount(), storageHealth.status(), storageHealth.available(),
                storageHealth.writable(), storageHealth.freeBytes(), storageHealth.totalBytes(),
                storageHealth.freePercent(), storageHealth.checkedAt(), asOf),
                Timestamp.valueOf(staleCutoff));
    }

    public ExportTaskOperationsIssuesVO issues() {
        LocalDateTime now = LocalDateTime.now();
        var staleTasks = jdbcTemplate.query("""
                SELECT uuid, task_name, requester_name, module_code, task_status, error_message,
                       create_time, heartbeat_at, completed_at
                FROM sys_export_task
                WHERE is_deleted = 0 AND task_status = 2
                  AND (heartbeat_at < ? OR (heartbeat_at IS NULL AND started_at < ?))
                ORDER BY heartbeat_at ASC, create_time ASC LIMIT 10
                """, this::mapIssue, Timestamp.valueOf(now.minusMinutes(staleMinutes)),
                Timestamp.valueOf(now.minusMinutes(staleMinutes)));
        var failedTasks = jdbcTemplate.query("""
                SELECT uuid, task_name, requester_name, module_code, task_status, error_message,
                       create_time, heartbeat_at, completed_at
                FROM sys_export_task
                WHERE is_deleted = 0 AND task_status = 4 AND completed_at >= ?
                ORDER BY completed_at DESC LIMIT 10
                """, this::mapIssue, Timestamp.valueOf(now.minusHours(24)));
        return new ExportTaskOperationsIssuesVO(staleTasks, failedTasks, now);
    }

    private ExportTaskOperationsIssueVO mapIssue(ResultSet resultSet, int rowNum) throws SQLException {
        return new ExportTaskOperationsIssueVO(
                resultSet.getString("uuid"), resultSet.getString("task_name"),
                resultSet.getString("requester_name"), resultSet.getString("module_code"),
                resultSet.getInt("task_status"), resultSet.getString("error_message"),
                localDateTime(resultSet.getTimestamp("create_time")),
                localDateTime(resultSet.getTimestamp("heartbeat_at")),
                localDateTime(resultSet.getTimestamp("completed_at")));
    }

    private LocalDateTime localDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
