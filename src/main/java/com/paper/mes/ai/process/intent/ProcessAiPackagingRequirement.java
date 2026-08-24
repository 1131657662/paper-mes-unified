package com.paper.mes.ai.process.intent;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;

public record ProcessAiPackagingRequirement(
        @NotNull @Pattern(regexp = "STRIP_SORT|REPACKAGE|FILM|BOX|OTHER") String type,
        @Pattern(regexp = "\\[金额]") String chargeToken,
        @NotNull @Pattern(regexp = "PIECE|TON|FIXED") String unit,
        @Pattern(regexp = "STANDARD|SPECIFIED") String quantityMode,
        boolean createsServiceStep) {

    /** Compatibility constructor for existing schema-1.0 results. */
    public ProcessAiPackagingRequirement(String type, String chargeToken, String unit,
                                         boolean createsServiceStep) {
        this(type, chargeToken, unit, "STANDARD", createsServiceStep);
    }
}
