package com.paper.mes.ai.process.model;

public record ProcessAiModelResult(
        String content,
        String model,
        String provider,
        String route,
        Integer inputTokens,
        Integer outputTokens) {
}
