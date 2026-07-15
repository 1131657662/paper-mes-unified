package com.paper.mes.backup.dto;

import com.paper.mes.backup.entity.BackupTask;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BackupTaskVO {

    private String uuid;
    private String taskType;
    private String backupId;
    private String taskStatus;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;
    private String operator;
    private String message;

    public static BackupTaskVO from(BackupTask task) {
        return BackupTaskVO.builder()
                .uuid(task.getUuid()).taskType(task.getTaskType()).backupId(task.getBackupId())
                .taskStatus(task.getTaskStatus()).startedAt(task.getStartedAt())
                .finishedAt(task.getFinishedAt()).durationMs(task.getDurationMs())
                .operator(task.getOperator()).message(task.getMessage()).build();
    }
}
