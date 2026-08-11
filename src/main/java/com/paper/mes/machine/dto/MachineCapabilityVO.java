package com.paper.mes.machine.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record MachineCapabilityVO(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String catalogUuid,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int stepType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String processCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String processName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String processCategory,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean defaultCapability,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int priority,
        Integer minWidth,
        Integer maxWidth,
        BigDecimal maxRollWeight,
        Integer maxDiameter,
        String remark
) { }
