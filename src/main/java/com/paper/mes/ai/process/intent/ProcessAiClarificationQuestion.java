package com.paper.mes.ai.process.intent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.util.List;

public record ProcessAiClarificationQuestion(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._:-]+") String questionId,
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z][A-Za-z0-9_.:-]{0,63}") String field,
        @Min(1) int parseRevision,
        @NotBlank @Size(max = 500) @Pattern(regexp = "[^\\p{Cntrl}]*") String question,
        @NotNull @Size(min = 1, max = 8) List<@Valid ProcessAiClarificationOption> options,
        boolean allowUnknown) {

    public ProcessAiClarificationQuestion {
        options = options == null ? List.of() : List.copyOf(options);
    }

}
