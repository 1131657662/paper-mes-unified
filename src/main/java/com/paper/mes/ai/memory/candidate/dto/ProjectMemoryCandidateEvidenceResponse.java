package com.paper.mes.ai.memory.candidate.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record ProjectMemoryCandidateEvidenceResponse(
        String uuid,
        String orderUuid,
        String orderNo,
        String parseId,
        String sourceType,
        String phrase,
        JsonNode context,
        JsonNode proposedValue,
        JsonNode finalValue,
        JsonNode difference,
        Boolean previewReady,
        String createdBy,
        LocalDateTime createdAt) {
}
