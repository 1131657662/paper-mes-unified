package com.paper.mes.ai.process.intent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Evidence shown in an understanding card; it is never an executable instruction. */
public record ProcessAiUnderstandingEvidence(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[^\\p{Cntrl}]*") String field,
        @NotBlank @Size(max = 500) @Pattern(regexp = "[^\\p{Cntrl}]*") String text,
        @NotBlank @Pattern(regexp = "CUSTOMER_TEXT|DB_FACT|APPROVED_MEMORY|DEFAULT|MODEL_INFERENCE")
        String sourceType,
        @NotBlank @Size(max = 128) @Pattern(regexp = "[A-Za-z0-9._:-]+") String sourceRef,
        @Size(max = 200) @Pattern(regexp = "[^\\p{Cntrl}]*") String normalizedRange) {
}
