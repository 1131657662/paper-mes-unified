package com.paper.mes.ai.process.intent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProcessAiClarificationOption(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._:-]+") String code,
        @NotBlank @Size(max = 120) @Pattern(regexp = "[^\\p{Cntrl}]*") String label) {
}
