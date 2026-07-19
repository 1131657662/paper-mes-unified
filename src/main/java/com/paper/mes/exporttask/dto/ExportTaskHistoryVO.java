package com.paper.mes.exporttask.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ExportTaskHistoryVO(
        List<ExportTaskVO> records,
        long total,
        long current,
        long size,
        LocalDateTime asOf
) {
}
