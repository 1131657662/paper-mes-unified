package com.paper.mes.ai.process.intent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

/** Semantic quantity requested by the customer; expansion is deterministic server-side. */
public record ProcessAiQuantityIntent(
        @NotBlank @Pattern(regexp = "REPEAT_WIDTH") String type,
        @NotNull @DecimalMin("1") @DecimalMax("10000") BigDecimal widthMm,
        @NotNull @Min(1) @jakarta.validation.constraints.Max(100) Integer count,
        @NotBlank @Pattern(regexp = "PER_SOURCE|TOTAL") String scope,
        List<@Valid ProcessAiSourceAllocation> sourceAllocation) {

    public ProcessAiQuantityIntent {
        sourceAllocation = sourceAllocation == null ? List.of() : List.copyOf(sourceAllocation);
    }
}
