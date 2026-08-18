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
        acceptedFieldPaths) {

    public ProcessAiConfirmRequest {
        acceptedFieldPaths = acceptedFieldPaths == null
                ? null : List.copyOf(acceptedFieldPaths);
    }
}
