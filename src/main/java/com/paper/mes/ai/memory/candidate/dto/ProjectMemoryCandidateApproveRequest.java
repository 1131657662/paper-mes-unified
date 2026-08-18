package com.paper.mes.ai.memory.candidate.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectMemoryCandidateApproveRequest(
        @NotBlank @Size(max = 32) String expectedMemoryVersion,
        @NotBlank @Size(max = 128) String idempotencyKey,
        @NotBlank @Size(max = 500) String reason,
        JsonNode candidate) {
}
