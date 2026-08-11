package com.paper.mes.machine.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record MachineVO(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String uuid,
        Integer version,
        String machineCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String machineName,
        Integer machineType,
        @Schema(allowableValues = {"MACHINE", "WORKSTATION"}) String resourceKind,
        Integer status,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        List<MachineCapabilityVO> capabilities
) { }
