package com.paper.mes.ai.process.intent;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record ProcessAiMeasurement(
        @NotNull @DecimalMin("0.01") @DecimalMax("10000") BigDecimal value,
        @NotNull @Pattern(regexp = "mm|inch") String unit,
        @Pattern(regexp = "EXPLICIT|DEFAULT|INHERITED") String source) {
}
