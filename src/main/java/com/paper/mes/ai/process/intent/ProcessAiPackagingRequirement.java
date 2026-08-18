package com.paper.mes.ai.process.intent;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;

public record ProcessAiPackagingRequirement(
        @NotNull @Pattern(regexp = "FILM|BOX|OTHER") String type,
        @Pattern(regexp = "\\[金额]") String chargeToken,
        @NotNull @Pattern(regexp = "PIECE|TON|FIXED") String unit,
        boolean createsServiceStep) {
}
