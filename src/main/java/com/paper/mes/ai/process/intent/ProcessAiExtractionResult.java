package com.paper.mes.ai.process.intent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProcessAiExtractionResult(
        @NotBlank @Size(max = 64) String parseId,
        @NotBlank @Pattern(regexp = "1\\.0") String schemaVersion,
        @NotNull @Size(min = 1, max = 100) List<@Valid ProcessAiAssignment> assignments,
        @NotNull @Size(max = 50) List<@Size(max = 500) String> unmappedText,
        @NotNull @Size(max = 20) List<@Size(max = 500) String> conflicts,
        boolean needsClarification,
        @NotNull @Size(max = 20) List<@Size(max = 500) String> clarificationQuestions) {

    public ProcessAiExtractionResult {
        assignments = assignments == null ? null : List.copyOf(assignments);
        unmappedText = unmappedText == null ? null : List.copyOf(unmappedText);
        conflicts = conflicts == null ? null : List.copyOf(conflicts);
        clarificationQuestions = clarificationQuestions == null ? null : List.copyOf(clarificationQuestions);
    }
}
