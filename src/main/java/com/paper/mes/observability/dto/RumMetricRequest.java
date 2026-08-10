package com.paper.mes.observability.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Exact allowlist for anonymous RUM data. Unknown JSON fields are rejected. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record RumMetricRequest(
        @NotBlank
        @Pattern(regexp = "CLS|FCP|INP|LCP|TTFB")
        String name,

        @NotNull
        @DecimalMin(value = "0", inclusive = true)
        @DecimalMax(value = "600000", inclusive = true)
        Double value,

        @NotBlank
        @Pattern(regexp = "good|needs-improvement|poor")
        String rating,

        @NotBlank
        @Size(max = 128)
        @Pattern(regexp = "(?:\\*|/(?:[a-z0-9:_*-]+(?:/[a-z0-9:_*-]+)*)?)")
        String route,

        @NotBlank
        @Pattern(regexp = "chrome|edge|firefox|safari|other")
        String browser,

        @NotBlank
        @Pattern(regexp = "(?:[0-9]{1,3}|unknown)")
        String browserVersion,

        @NotBlank
        @Pattern(regexp = "low|mid|high|unknown")
        String deviceTier,

        @NotBlank
        @Pattern(regexp = "slow-2g|2g|3g|4g|unknown")
        String networkType
) {
    @JsonAnySetter
    public void rejectUnknownField(String field, Object ignoredValue) {
        throw new IllegalArgumentException("Unsupported telemetry field: " + field);
    }
}
