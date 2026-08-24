package com.paper.mes.ai.process.intent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProcessAiEvidence(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[^\\p{Cntrl}]*") String field,
        @NotBlank @Size(max = 500) @Pattern(regexp = "[^\\p{Cntrl}]*") String text,
        @Pattern(regexp = "CUSTOMER_TEXT|DB_FACT|APPROVED_MEMORY|DEFAULT|MODEL_INFERENCE")
        String sourceType,
        @Size(max = 128) @Pattern(regexp = "[A-Za-z0-9._:-]+") String sourceRef) {

    /** Compatibility constructor for legacy model fixtures; the verifier will re-check it. */
    public ProcessAiEvidence(String field, String text) {
        this(field, text, "CUSTOMER_TEXT", "customerRequirement");
    }
}
