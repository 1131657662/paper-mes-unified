package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
class ProcessPlanDraftStore {

    private final OriginalRollMapper rollMapper;
    private final ProcessConfigDraftMapper draftMapper;
    private final ObjectMapper objectMapper;

    ProcessPlanRollSelection requireRolls(String orderUuid, List<String> rollUuids,
                                          boolean includeOrderSources) {
        if (includeOrderSources) {
            Map<String, OriginalRoll> orderRolls = orderRollMap(orderUuid);
            return new ProcessPlanRollSelection(requireTargets(rollUuids, orderRolls), orderRolls);
        }
        return new ProcessPlanRollSelection(requireTargetRows(orderUuid, rollUuids), Map.of());
    }

    private Map<String, OriginalRoll> requireTargetRows(String orderUuid, List<String> rollUuids) {
        Map<String, OriginalRoll> result = new HashMap<>();
        for (OriginalRoll roll : rollMapper.selectBatchIds(rollUuids)) {
            if (orderUuid.equals(roll.getOrderUuid())) {
                result.put(roll.getUuid(), roll);
            }
        }
        if (result.size() != rollUuids.size()) {
            throw new BusinessException(ErrorCode.E002, "部分原纸明细不存在或不属于当前加工单");
        }
        return result;
    }

    private Map<String, OriginalRoll> orderRollMap(String orderUuid) {
        List<OriginalRoll> rolls = rollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                .eq(OriginalRoll::getOrderUuid, orderUuid));
        Map<String, OriginalRoll> result = new HashMap<>();
        rolls.forEach(roll -> result.put(roll.getUuid(), roll));
        return result;
    }

    private Map<String, OriginalRoll> requireTargets(List<String> rollUuids,
                                                     Map<String, OriginalRoll> orderRolls) {
        Map<String, OriginalRoll> targets = new HashMap<>();
        for (String rollUuid : rollUuids) {
            OriginalRoll roll = orderRolls.get(rollUuid);
            if (roll == null) {
                throw new BusinessException(ErrorCode.E002, "部分原纸明细不存在或不属于当前加工单");
            }
            targets.put(rollUuid, roll);
        }
        return targets;
    }

    ProcessConfigDraft findDraft(String orderUuid, String rollUuid) {
        return draftMapper.selectOne(baseDraftQuery(orderUuid)
                .eq(ProcessConfigDraft::getOriginalUuid, rollUuid)
                .last("LIMIT 1"));
    }

    Map<String, ProcessConfigDraft> findDrafts(String orderUuid, List<String> rollUuids) {
        List<ProcessConfigDraft> drafts = draftMapper.selectList(baseDraftQuery(orderUuid)
                .in(ProcessConfigDraft::getOriginalUuid, rollUuids));
        Map<String, ProcessConfigDraft> result = new HashMap<>();
        drafts.forEach(draft -> result.put(draft.getOriginalUuid(), draft));
        return result;
    }

    void persist(String orderUuid, PreparedProcessPlan prepared) {
        updateMachine(prepared.candidate());
        upsertDraft(orderUuid, prepared);
    }

    private void updateMachine(ProcessPlanSaveCandidate candidate) {
        OriginalRoll roll = candidate.roll();
        roll.setMachineUuid(candidate.plan().getMachineUuid());
        roll.setUpdateBy(null);
        roll.setUpdateTime(null);
        ConcurrencyGuard.requireRowUpdated(rollMapper.updateById(roll));
    }

    private void upsertDraft(String orderUuid, PreparedProcessPlan prepared) {
        ProcessPlanSaveCandidate candidate = prepared.candidate();
        ProcessPlanDTO plan = candidate.plan();
        PlanPreviewVO preview = prepared.preview();
        ProcessConfigDraft draft = candidate.existingDraft();
        if (draft == null) {
            draft = new ProcessConfigDraft();
            draft.setOrderUuid(orderUuid);
            draft.setOriginalUuid(candidate.roll().getUuid());
        }
        applyDraftValues(draft, plan, preview);
        if (draft.getUuid() == null) {
            ConcurrencyGuard.requireRowUpdated(draftMapper.insert(draft));
        } else {
            draft.setUpdateBy(null);
            draft.setUpdateTime(null);
            ConcurrencyGuard.requireRowUpdated(draftMapper.updateById(draft));
        }
    }

    private void applyDraftValues(ProcessConfigDraft draft, ProcessPlanDTO plan, PlanPreviewVO preview) {
        draft.setProcessMode(plan.getProcessMode());
        draft.setMainStepType(plan.getMainStepType());
        draft.setConfigJson(toJson(plan));
        draft.setPreviewJson(toJson(preview));
        draft.setConfigStatus(preview.isReady() ? 1 : 0);
        draft.setLastError(preview.isReady() ? null : String.join("；", preview.getErrors()));
    }

    private LambdaQueryWrapper<ProcessConfigDraft> baseDraftQuery(String orderUuid) {
        return new LambdaQueryWrapper<ProcessConfigDraft>()
                .eq(ProcessConfigDraft::getOrderUuid, orderUuid);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("工艺配置草稿序列化失败");
        }
    }
}
