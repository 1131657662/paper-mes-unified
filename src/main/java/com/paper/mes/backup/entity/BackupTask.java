package com.paper.mes.backup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_backup_task")
public class BackupTask extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String taskType;
    private String backupId;
    private String taskStatus;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;
    private String operator;
    private String message;
}
