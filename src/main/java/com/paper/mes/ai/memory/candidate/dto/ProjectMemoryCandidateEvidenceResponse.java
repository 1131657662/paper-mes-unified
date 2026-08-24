package com.paper.mes.ai.memory.candidate.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectMemoryCandidateEvidenceResponse(
        String uuid,
        String phrase,
        String sourceType,
        JsonNode proposedValue,
        JsonNode finalValue,
        JsonNode difference,
        Boolean previewReady,
        LocalDateTime createdAt) {
}
