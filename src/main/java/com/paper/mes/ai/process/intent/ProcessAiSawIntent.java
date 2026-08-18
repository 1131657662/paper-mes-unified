package com.paper.mes.ai.process.intent;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProcessAiSawIntent(
        @NotBlank @Pattern(regexp = "CUTS|EQUAL_SPLIT|EXPLICIT_WIDTHS") String type,
        @Min(0) @Max(499) Integer knifeCount,
        @Size(min = 1, max = 500) List<@Min(1) @Max(10000) Integer> widths,
        @Pattern(regexp = "mm") String unit) {

    public ProcessAiSawIntent {
        widths = widths == null ? null : List.copyOf(widths);
    }
}
