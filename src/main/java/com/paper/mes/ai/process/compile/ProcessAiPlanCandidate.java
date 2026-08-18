package com.paper.mes.ai.process.compile;

import com.paper.mes.processorder.dto.ProcessPlanDTO;

import java.util.List;

record ProcessAiPlanCandidate(
        String ownerRollRef,
        String originalUuid,
        List<String> coveredOriginalUuids,
        ProcessPlanDTO plan) {

    ProcessAiPlanCandidate {
        coveredOriginalUuids = List.copyOf(coveredOriginalUuids);
    }
}
