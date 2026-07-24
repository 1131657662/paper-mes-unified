package com.paper.mes.report.savedview.dto;

import com.paper.mes.report.dto.ReportQuery;

import java.time.LocalDateTime;
import java.util.List;

public record ReportSavedViewVO(
        String uuid,
        String viewName,
        String reportPath,
        ReportQuery reportQuery,
        String dimensionCode,
        List<String> metricCodes,
        Integer isDefault,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        Integer version
) {
}
