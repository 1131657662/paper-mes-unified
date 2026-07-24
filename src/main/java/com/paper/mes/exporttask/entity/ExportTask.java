package com.paper.mes.exporttask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_export_task")
public class ExportTask extends BaseEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String requestId;
    private String taskType;
    private String moduleCode;
    private String operationCode;
    private String taskName;
    private String sourceUuid;
    private String sourcePath;
    private String requestPayload;
    private String querySnapshotUuid;
    private String metricReleaseUuid;
    private String requesterUuid;
    private String requesterName;
    /** 1等待 2执行中 3成功 4失败 */
    private Integer taskStatus;
    private Integer progress;
    private String fileName;
    /** Root-relative artifact key; legacy absolute paths are resolved for compatibility. */
    private String filePath;
    private String contentType;
    private Long fileSize;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime expiresAt;
    private Integer attemptCount;
    private Integer maxAttempts;
    private LocalDateTime heartbeatAt;
    private String workerId;
    private LocalDateTime downloadedAt;
    private Integer downloadCount;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
}
