package com.paper.mes.machine.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MachineVO(
        String uuid,
        Integer version,
        String machineCode,
        String machineName,
        Integer machineType,
        String resourceKind,
        Integer status,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        List<MachineCapabilityVO> capabilities
) { }
