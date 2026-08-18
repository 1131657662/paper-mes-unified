package com.paper.mes.ai.process.context;

import com.paper.mes.processorder.dto.ProcessPlanDTO;

public record ProcessAiBaselinePlan(
        String ownerRollRef,
        String originalUuid,
        Integer processMode,
        Integer mainStepType,
        boolean route,
        ProcessPlanDTO plan) {
}
