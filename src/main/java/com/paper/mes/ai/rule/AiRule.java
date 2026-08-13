package com.paper.mes.ai.rule;

import java.util.List;

public record AiRule(
        String ruleId,
        String version,
        String status,
        String module,
        String operation,
        List<String> statuses,
        List<String> roles,
        List<String> conditions,
        String decision,
        String effect,
        String title,
        List<String> keywords,
        String answer,
        List<String> safeNextSteps,
        List<String> prohibited,
        String effectiveDate,
        String businessOwner,
        String reviewer,
        String source,
        int priority,
        int specificity,
        String supersedes,
        String conflictResolutionId,
        String checksum) {
}
