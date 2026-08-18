package com.paper.mes.ai.process.intent;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProcessAiWidthRule(
        @NotBlank @Pattern(regexp = "EXPLICIT|AVERAGE|KNIFE_COUNT|KEEP_SPEC") String type,
        @Size(min = 1, max = 500) List<@Min(1) @Max(10000) Integer> values,
        @Pattern(regexp = "mm") String unit,
        @Min(0) @Max(499) Integer knifeCount) {

    public ProcessAiWidthRule {
        values = values == null ? null : List.copyOf(values);
    }
}
