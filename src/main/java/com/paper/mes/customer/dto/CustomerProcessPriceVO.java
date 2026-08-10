package com.paper.mes.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CustomerProcessPriceVO(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String catalogUuid,
        Integer stepType,
        String processCode,
        String processName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"PIECE", "TON", "FIXED"})
        String billingBasis,
        String billingUnitName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal price,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean defaultOption) {
}
