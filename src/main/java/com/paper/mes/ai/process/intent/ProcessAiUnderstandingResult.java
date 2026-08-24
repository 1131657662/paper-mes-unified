package com.paper.mes.ai.process.intent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** A safe, non-executable understanding card returned while the model is uncertain. */
public record ProcessAiUnderstandingResult(
        @NotBlank @Size(max = 64) String parseId,
        @NotBlank @Pattern(regexp = "2\\.0") String schemaVersion,
        @NotBlank @Size(max = 2_000) @Pattern(regexp = "[^\\p{Cntrl}]*") String conclusion,
        @NotNull @Size(max = 30) List<@Valid ProcessAiUnderstandingEvidence> evidence,
        @NotNull @Size(max = 30) List<@Size(max = 500) @Pattern(regexp = "[^\\p{Cntrl}]*") String> assumptions,
        @NotNull @Size(max = 30) List<@Size(max = 500) @Pattern(regexp = "[^\\p{Cntrl}]*") String> risks,
        @NotNull @Size(max = 8) List<@Valid ProcessAiClarificationQuestion> clarificationQuestions,
        boolean needsClarification) {

    public ProcessAiUnderstandingResult {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
        risks = risks == null ? List.of() : List.copyOf(risks);
        clarificationQuestions = clarificationQuestions == null
                ? List.of() : List.copyOf(clarificationQuestions);
    }
}
