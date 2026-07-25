package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import com.paper.mes.processorder.entity.ProcessOrder;

import java.util.List;
import java.util.Map;
import java.util.Set;

record ProcessPlanSaveCandidate(
        OriginalRoll roll,
        ProcessPlanDTO plan,
        ProcessConfigDraft existingDraft) {
}

record PreparedProcessPlan(
        ProcessPlanSaveCandidate candidate,
        PlanPreviewVO preview) {
}

record ProcessPlanRollSelection(
        Map<String, OriginalRoll> targets,
        Map<String, OriginalRoll> sourceRolls) {
}

record ProcessPlanSaveWork(
        List<ProcessPlanSaveCandidate> candidates,
        Map<String, OriginalRoll> sourceRolls) {
}

record ProcessPlanDraftPreviewContext(
        ProcessOrder order,
        Map<String, OriginalRoll> sourceRolls,
        Set<String> configuredServiceRollUuids) {
}
