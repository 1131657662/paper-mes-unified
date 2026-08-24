package com.paper.mes.ai.process.stream.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProcessAiParseStreamRequest(
        @NotNull @Min(0) Integer expectedVersion,
        @NotBlank @Size(max = 64) String conversationId,
        @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9._:-]+") String idempotencyKey,
        @NotBlank @Pattern(regexp = "START|CLARIFY") String action,
        @Size(max = 2_000) String message,
        @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._:-]+") String questionId,
        @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._:-]+") String parseId,
        @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._:-]+") String answerCode,
        @Size(max = 500) @Pattern(regexp = "[^\\p{Cntrl}]*") String answerText,
        @Min(1) Integer parseRevision) {

    public ProcessAiParseStreamRequest(Integer expectedVersion, String conversationId,
                                       String idempotencyKey, String action, String message) {
        this(expectedVersion, conversationId, idempotencyKey, action, message,
                null, null, null, null, null);
    }
}
