package com.paper.mes.ai.process.intent;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Customer-facing labels only; physical dimensions and weights remain system-owned. */
public record ProcessAiCustomerSpec(
        @Min(0) @Max(499) Integer outputIndex,
        @Size(max = 100) @Pattern(regexp = "[^\\p{Cntrl}]*") String paperName,
        @Positive @Max(10000) Integer gramWeight,
        @Positive @Max(10000) Integer finishWidth,
        @Size(max = 255) @Pattern(regexp = "[^\\p{Cntrl}]*") String overrideReason) {

    public boolean hasValue() {
        return paperName != null || gramWeight != null || finishWidth != null;
    }
}
