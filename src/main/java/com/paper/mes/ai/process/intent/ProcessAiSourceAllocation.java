package com.paper.mes.ai.process.intent;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProcessAiSourceAllocation(
        @NotBlank @Pattern(regexp = "R[1-9]\\d{0,2}") String sourceRollRef,
        @Min(1) int count) {
}
