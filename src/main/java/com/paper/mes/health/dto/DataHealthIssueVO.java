package com.paper.mes.health.dto;

public record DataHealthIssueVO(
        String issueType,
        String severity,
        String businessType,
        String businessUuid,
        String businessNo,
        String title,
        String detail,
        String repairAction
) {
}
