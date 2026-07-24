package com.paper.mes.machine.dto;

import java.math.BigDecimal;

public record MachineCapabilityVO(
        String catalogUuid,
        int stepType,
        String processCode,
        String processName,
        String processCategory,
        boolean defaultCapability,
        int priority,
        Integer minWidth,
        Integer maxWidth,
        BigDecimal maxRollWeight,
        Integer maxDiameter,
        String remark
) { }
