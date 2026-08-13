package com.paper.mes.ai.model;

import java.util.List;

public record AiModelResult(String answer, List<String> citationRuleIds) {
}
