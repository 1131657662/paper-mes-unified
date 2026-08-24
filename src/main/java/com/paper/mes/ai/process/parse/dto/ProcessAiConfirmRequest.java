package com.paper.mes.ai.process.parse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProcessAiConfirmRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._:-]+")
        String conversationId,
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._:-]+")
        String parseId,
        @NotNull @Min(0) Integer expectedVersion,
        @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9._:-]+")
        String applyIdempotencyKey,
        @NotNull @Size(min = 1, max = 100) List<@NotBlank @Size(max = 160) String>
        acceptedFieldPaths,
        @Min(1) Integer parseRevision,
        @Size(min = 64, max = 64) @Pattern(regexp = "[a-fA-F0-9]{64}") String previewHash,
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 64) String>
        acknowledgedDefaultIds) {

    public ProcessAiConfirmRequest(
            String conversationId, String parseId, Integer expectedVersion,
            String applyIdempotencyKey, List<String> acceptedFieldPaths) {
        this(conversationId, parseId, expectedVersion, applyIdempotencyKey,
                acceptedFieldPaths, null, null, List.of());
    }

    public ProcessAiConfirmRequest {
        acceptedFieldPaths = acceptedFieldPaths == null
                ? null : List.copyOf(acceptedFieldPaths);
        acknowledgedDefaultIds = acknowledgedDefaultIds == null
                ? List.of() : List.copyOf(acknowledgedDefaultIds);
    }
}
