package com.paper.mes.exporttask.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ExportTaskOperationsIssuesVO(
        List<ExportTaskOperationsIssueVO> staleTasks,
        List<ExportTaskOperationsIssueVO> failedTasks,
        LocalDateTime asOf
) {
}
