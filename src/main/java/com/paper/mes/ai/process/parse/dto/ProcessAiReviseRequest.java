package com.paper.mes.ai.process.parse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProcessAiReviseRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._:-]+") String conversationId,
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._:-]+") String parseId,
        @NotNull @Min(0) Integer expectedVersion,
        @NotNull @Min(1) Integer parseRevision,
        @NotNull @Size(min = 1, max = 20) List<@Valid ProcessAiCorrection> corrections) {

    public ProcessAiReviseRequest {
        corrections = corrections == null ? List.of() : List.copyOf(corrections);
    }
}
