package com.paper.mes.processorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
class ProcessPlanSavePolicy {

    private static final int REWIND_MODE_MULTI_SOURCE = 5;

    private final ObjectMapper objectMapper;

    void requireDistinctTargets(List<String> rollUuids) {
        if (rollUuids == null || rollUuids.isEmpty()) {
            throw new BusinessException(ErrorCode.E003, "请选择需要应用加工方案的母卷");
        }
        if (rollUuids.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new BusinessException(ErrorCode.E003, "批量加工方案包含无效母卷编号");
        }
        if (new HashSet<>(rollUuids).size() != rollUuids.size()) {
            throw new BusinessException(ErrorCode.E003, "批量加工方案存在重复母卷");
        }
    }

    void requireGenericBatchAllowed(ProcessPlanDTO plan) {
        requirePlan(plan);
        if (Integer.valueOf(REWIND_MODE_MULTI_SOURCE).equals(plan.getRewindMode())) {
            throw new BusinessException(ErrorCode.E003, "多来源复卷不能使用通用批量应用，请逐卷保存");
        }
    }

    void requireSavable(ProcessPlanSaveCandidate candidate) {
        requirePlan(candidate.plan());
        ProcessModePolicy.requireValid(candidate.plan().getProcessMode(), candidate.plan().getMainStepType());
        requireProcessMatches(candidate.roll(), candidate.plan());
        requireNotRouteDraft(candidate.existingDraft());
    }

    private void requirePlan(ProcessPlanDTO plan) {
        if (plan == null || plan.getProcessMode() == null) {
            throw new BusinessException(ErrorCode.E003, "加工方案及加工方式不能为空");
        }
    }

    private void requireProcessMatches(OriginalRoll roll, ProcessPlanDTO plan) {
        boolean sameMode = Objects.equals(roll.getProcessMode(), plan.getProcessMode());
        boolean sameStep = Objects.equals(roll.getMainStepType(), plan.getMainStepType());
        if (!sameMode || !sameStep) {
            throw new BusinessException(ErrorCode.E003, "加工方案与母卷当前加工方式或主工艺不一致，请刷新后重试");
        }
    }

    private void requireNotRouteDraft(ProcessConfigDraft draft) {
        if (draft == null) {
            return;
        }
        JsonNode config = readConfig(draft);
        if (config.has("stages")) {
            throw new BusinessException(ErrorCode.E003, "该母卷已配置链式工艺，不能用单道方案覆盖");
        }
    }

    private JsonNode readConfig(ProcessConfigDraft draft) {
        try {
            JsonNode config = objectMapper.readTree(draft.getConfigJson());
            if (config == null || !config.isObject()) {
                throw new BusinessException(ErrorCode.E003, "现有工艺草稿无法识别，禁止覆盖");
            }
            return config;
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.E003, "现有工艺草稿解析失败，禁止覆盖");
        }
    }
}
