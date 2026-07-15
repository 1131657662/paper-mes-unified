package com.paper.mes.backup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.backup.dto.BackupTaskVO;
import com.paper.mes.backup.entity.BackupTask;
import com.paper.mes.backup.mapper.BackupTaskMapper;
import com.paper.mes.common.ConcurrencyGuard;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BackupTaskHistoryService {

    private final BackupTaskMapper taskMapper;
    private final ApplicationEventPublisher eventPublisher;

    public BackupTaskHistoryService(BackupTaskMapper taskMapper, ApplicationEventPublisher eventPublisher) {
        this.taskMapper = taskMapper;
        this.eventPublisher = eventPublisher;
    }

    public String start(String taskType, String backupId, String operator) {
        BackupTask task = new BackupTask();
        task.setTaskType(taskType);
        task.setBackupId(backupId);
        task.setTaskStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        task.setOperator(operator);
        task.setMessage("任务执行中");
        taskMapper.insert(task);
        return task.getUuid();
    }

    public void finish(String taskUuid, String backupId, boolean success, String message) {
        BackupTask task = taskMapper.selectById(taskUuid);
        if (task == null) return;
        LocalDateTime finishedAt = LocalDateTime.now();
        task.setBackupId(backupId);
        task.setTaskStatus(success ? "SUCCESS" : "FAILED");
        task.setFinishedAt(finishedAt);
        task.setDurationMs(Duration.between(task.getStartedAt(), finishedAt).toMillis());
        task.setMessage(truncate(message));
        ConcurrencyGuard.requireRowUpdated(taskMapper.updateById(task));
        if (!success) {
            publishFailure(task);
        }
    }

    public int recoverInterruptedTasks() {
        List<BackupTask> runningTasks = taskMapper.selectList(new LambdaQueryWrapper<BackupTask>()
                .eq(BackupTask::getTaskStatus, "RUNNING")
                .orderByAsc(BackupTask::getStartedAt));
        LocalDateTime recoveredAt = LocalDateTime.now();
        int recovered = 0;
        for (BackupTask task : runningTasks) {
            markInterrupted(task, recoveredAt);
            if (taskMapper.updateById(task) == 1) {
                recovered++;
                publishFailure(task);
            }
        }
        return recovered;
    }

    public List<BackupTaskVO> recent() {
        return taskMapper.selectList(new LambdaQueryWrapper<BackupTask>()
                        .orderByDesc(BackupTask::getStartedAt).last("LIMIT 50"))
                .stream().map(BackupTaskVO::from).toList();
    }

    public BackupAutomaticHistory automaticHistory() {
        List<BackupTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<BackupTask>()
                .eq(BackupTask::getTaskType, "AUTO_BACKUP")
                .orderByDesc(BackupTask::getStartedAt).last("LIMIT 50"));
        if (tasks.isEmpty()) return new BackupAutomaticHistory(null, null, 0);
        long failures = tasks.stream().filter(task -> !"RUNNING".equals(task.getTaskStatus()))
                .takeWhile(task -> "FAILED".equals(task.getTaskStatus())).count();
        BackupTask latest = tasks.getFirst();
        return new BackupAutomaticHistory(latest.getStartedAt(), latest.getTaskStatus(), failures);
    }

    private String truncate(String message) {
        if (message == null) return null;
        return message.length() <= 255 ? message : message.substring(0, 255);
    }

    private void markInterrupted(BackupTask task, LocalDateTime recoveredAt) {
        task.setTaskStatus("FAILED");
        task.setFinishedAt(recoveredAt);
        task.setDurationMs(task.getStartedAt() == null ? 0L
                : Math.max(0L, Duration.between(task.getStartedAt(), recoveredAt).toMillis()));
        task.setMessage("服务重启时检测到任务中断，已自动标记失败，请核查备份文件后重试");
    }

    private void publishFailure(BackupTask task) {
        eventPublisher.publishEvent(new BackupTaskFailedEvent(
                task.getUuid(), task.getTaskType(), task.getBackupId(), task.getStartedAt()));
    }
}
