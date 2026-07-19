package com.paper.mes.exporttask.dto;

import java.time.LocalDateTime;

public record ExportTaskOperationsIssueVO(
        String uuid,
        String taskName,
        String requesterName,
        String moduleCode,
        Integer taskStatus,
        String errorMessage,
        LocalDateTime createTime,
        LocalDateTime heartbeatAt,
        LocalDateTime completedAt
) {
}
