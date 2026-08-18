package com.paper.mes.ai.memory.candidate.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record ProjectMemoryCandidateResponse(
        String uuid,
        String memoryId,
        String candidateType,
        JsonNode candidate,
        String status,
        int distinctOrderCount,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt,
        LocalDateTime expiresAt,
        String reviewedBy,
        String reviewNotes,
        LocalDateTime reviewedAt) {
}
