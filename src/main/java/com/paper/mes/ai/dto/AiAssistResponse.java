package com.paper.mes.ai.dto;

import com.paper.mes.ai.config.AiDataMode;

import java.util.List;

public record AiAssistResponse(
        String requestId,
        String decision,
        String confidence,
        String answer,
        List<String> safeNextSteps,
        List<AiCitation> citations,
        AiDataMode dataMode,
        String provider) {
}
