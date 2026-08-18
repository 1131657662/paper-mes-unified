package com.paper.mes.ai.process.intent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProcessAiDiameterRule(
        @NotBlank @Pattern(regexp = "WEIGHT_SPLIT|KEEP_SPEC|EXPLICIT") String type,
        @Min(1) @Max(100) Integer parts,
        @Size(min = 1, max = 100) List<@Min(0) @Max(100) BigDecimal> ratios,
        @Valid ProcessAiMeasurement targetDiameter) {

    public ProcessAiDiameterRule {
        ratios = ratios == null ? null : List.copyOf(ratios);
        if (targetDiameter != null && targetDiameter.value() == null
                && targetDiameter.unit() == null && targetDiameter.source() == null) {
            targetDiameter = null;
        }
    }
}
