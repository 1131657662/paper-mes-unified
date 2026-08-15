package com.paper.mes.ai.memory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectMemoryRollbackRequest(
        @NotBlank @Size(max = 32) String expectedMemoryVersion,
        @NotBlank @Size(max = 32) String targetMemoryVersion,
        @NotBlank @Size(max = 128) String idempotencyKey,
        @NotBlank @Size(max = 500) String reason) {
}
