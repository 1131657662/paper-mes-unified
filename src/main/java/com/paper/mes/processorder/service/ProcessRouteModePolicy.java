package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.springframework.stereotype.Component;

@Component
public class ProcessRouteModePolicy {

    public void requireCompatible(OriginalRoll roll, ProcessRoutePreviewDTO dto) {
        if (!Integer.valueOf(ProcessModePolicy.STANDARD).equals(roll.getProcessMode())) {
            throw new BusinessException(ErrorCode.E003, "链式工艺仅支持标准加工卷");
        }
        Integer firstStepType = firstStepType(dto);
        if (roll.getMainStepType() != null && !roll.getMainStepType().equals(firstStepType)) {
            throw new BusinessException(ErrorCode.E003, "链式工艺首道工艺与母卷主工艺不一致");
        }
    }

    private Integer firstStepType(ProcessRoutePreviewDTO dto) {
        if (dto == null || dto.getStages() == null || dto.getStages().isEmpty()) {
            throw new BusinessException(ErrorCode.E003, "工艺路线不能为空");
        }
        return dto.getStages().getFirst().getStepType();
    }
}
