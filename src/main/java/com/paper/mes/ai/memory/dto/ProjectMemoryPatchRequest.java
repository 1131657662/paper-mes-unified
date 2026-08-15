package com.paper.mes.ai.memory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProjectMemoryPatchRequest(
        @NotBlank @Size(max = 32) String expectedMemoryVersion,
        @NotEmpty @Size(max = 20) List<@Valid ProjectMemoryPatchOperation> operations,
        @NotBlank @Size(max = 128) String idempotencyKey,
        @NotBlank @Size(max = 500) String reason) {
}
