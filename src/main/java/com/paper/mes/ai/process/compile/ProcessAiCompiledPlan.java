package com.paper.mes.ai.process.compile;

import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;

import java.util.List;

public record ProcessAiCompiledPlan(
        String ownerRollRef,
        String originalUuid,
        List<String> coveredOriginalUuids,
        ProcessPlanDTO plan,
        PlanPreviewVO preview) {

    public ProcessAiCompiledPlan {
        coveredOriginalUuids = List.copyOf(coveredOriginalUuids);
    }
}
