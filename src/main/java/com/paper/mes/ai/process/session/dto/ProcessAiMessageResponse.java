package com.paper.mes.ai.process.session.dto;

import java.time.LocalDateTime;

public record ProcessAiMessageResponse(
        int sequenceNo,
        String role,
        String status,
        String content,
        String structuredResult,
        LocalDateTime createdAt) {
}
