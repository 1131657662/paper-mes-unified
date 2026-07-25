package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessPlanBatchSaveDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.ProcessPlanItemsBatchSaveDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
class ProcessPlanSaveWorkLoader {

    private static final int REWIND_MODE_MULTI_SOURCE = 5;

    private final ProcessPlanDraftStore store;
    private final ProcessPlanBatchTargetFactory targetFactory;

    ProcessPlanSaveWork forPreview(String orderUuid, String rollUuid, ProcessPlanDTO plan) {
        ProcessPlanRollSelection rolls = store.requireRolls(
                orderUuid, List.of(rollUuid), isMultiSource(plan));
        ProcessPlanSaveCandidate candidate = new ProcessPlanSaveCandidate(
                rolls.targets().get(rollUuid), plan, null);
        return new ProcessPlanSaveWork(List.of(candidate), rolls.sourceRolls());
    }

    ProcessPlanSaveWork forSingleSave(String orderUuid, String rollUuid, ProcessPlanDTO plan) {
        ProcessPlanSaveWork work = forPreview(orderUuid, rollUuid, plan);
        ProcessPlanSaveCandidate candidate = work.candidates().getFirst();
        ProcessConfigDraft draft = store.findDraft(orderUuid, rollUuid);
        return new ProcessPlanSaveWork(
                List.of(new ProcessPlanSaveCandidate(candidate.roll(), plan, draft)), work.sourceRolls());
    }

    ProcessPlanSaveWork forBatch(String orderUuid, ProcessPlanBatchSaveDTO dto) {
        List<String> ids = dto.getOriginalUuids();
        ProcessPlanRollSelection rolls = store.requireRolls(orderUuid, ids, isMultiSource(dto.getPlan()));
        Map<String, ProcessConfigDraft> drafts = store.findDrafts(orderUuid, ids);
        List<ProcessPlanSaveCandidate> candidates = ids.stream()
                .map(id -> new ProcessPlanSaveCandidate(
                        rolls.targets().get(id), targetFactory.create(dto.getPlan(), id), drafts.get(id)))
                .toList();
        return new ProcessPlanSaveWork(candidates, rolls.sourceRolls());
    }

    ProcessPlanSaveWork forItems(String orderUuid, ProcessPlanItemsBatchSaveDTO dto) {
        List<String> ids = dto.getItems().stream().map(item -> item.getOriginalUuid()).toList();
        boolean needsOrderSources = dto.getItems().stream().anyMatch(item -> isMultiSource(item.getPlan()));
        ProcessPlanRollSelection rolls = store.requireRolls(orderUuid, ids, needsOrderSources);
        Map<String, ProcessConfigDraft> drafts = store.findDrafts(orderUuid, ids);
        List<ProcessPlanSaveCandidate> candidates = dto.getItems().stream()
                .map(item -> new ProcessPlanSaveCandidate(
                        rolls.targets().get(item.getOriginalUuid()), item.getPlan(),
                        drafts.get(item.getOriginalUuid())))
                .toList();
        return new ProcessPlanSaveWork(candidates, rolls.sourceRolls());
    }

    private boolean isMultiSource(ProcessPlanDTO plan) {
        return plan != null && Integer.valueOf(REWIND_MODE_MULTI_SOURCE).equals(plan.getRewindMode());
    }
}
