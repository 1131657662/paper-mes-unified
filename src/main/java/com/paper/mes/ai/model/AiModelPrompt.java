package com.paper.mes.ai.model;

import java.util.List;

public record AiModelPrompt(
        String pageTemplate,
        List<String> ruleIds,
        String ruleTitle,
        String ruleAnswer,
        List<String> safeNextSteps) {
}
