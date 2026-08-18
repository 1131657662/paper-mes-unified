package com.paper.mes.ai.process.intent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProcessAiEvidence(
        @NotBlank @Size(max = 100) String field,
        @NotBlank @Size(max = 500) String text) {
}
