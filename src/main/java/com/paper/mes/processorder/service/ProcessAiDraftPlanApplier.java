package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanBatchItemDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.ProcessPlanItemsBatchSaveDTO;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
class ProcessAiDraftPlanApplier {

    private final ProcessConfigDraftMapper draftMapper;
    private final ProcessPlanMapper planMapper;
    private final ProcessPlanSaveWorkLoader workLoader;
    private final ProcessPlanSavePolicy savePolicy;
    private final ProcessPlanDraftPreviewer previewer;
    private final ProcessPlanDraftStore store;
    private final ProcessAiPlanFieldMerger fieldMerger;
    private final ObjectMapper objectMapper;

    Map<String, ProcessAiCompiledPlan> apply(ProcessOrder order,
                                             ProcessAiDraftApplyCommand command,
                                             List<ProcessAiCompiledPlan> selected) {
        if (selected.isEmpty()) return Map.of();
        ProcessPlanSaveWork work = workLoader.forItems(command.orderUuid(), batch(command, selected));
        Map<String, ProcessAiCompiledPlan> source = index(selected);
        Map<String, ProcessAiCompiledPlan> result = new LinkedHashMap<>();
        var context = previewer.createContext(order, work.sourceRolls(), work.candidates());
        for (ProcessPlanSaveCandidate candidate : work.candidates()) {
            candidate.roll().setProcessMode(candidate.plan().getProcessMode());
            candidate.roll().setMainStepType(candidate.plan().getMainStepType());
            FinishConfigQuantityValidator.requireWithinLimit(candidate.plan());
            savePolicy.requireSavable(candidate);
            var preview = previewer.preview(context, candidate.roll(), candidate.plan());
            requireReady(candidate.roll().getRowSort(), preview);
            store.persist(command.orderUuid(), new PreparedProcessPlan(candidate, preview));
            saveIntent(command, candidate.roll().getUuid());
            ProcessAiCompiledPlan original = source.get(candidate.roll().getUuid());
            result.put(candidate.roll().getUuid(), new ProcessAiCompiledPlan(
                    original.ownerRollRef(), original.originalUuid(), original.coveredOriginalUuids(),
                    candidate.plan(), preview));
        }
        return result;
    }

    private ProcessPlanItemsBatchSaveDTO batch(ProcessAiDraftApplyCommand command,
                                                List<ProcessAiCompiledPlan> selected) {
        Map<String, ProcessConfigDraft> current = currentDrafts(command.orderUuid(), selected);
        ProcessPlanItemsBatchSaveDTO dto = new ProcessPlanItemsBatchSaveDTO();
        dto.setExpectedVersion(command.expectedVersion());
        dto.setItems(selected.stream().map(candidate -> item(
                command, candidate, current.get(candidate.originalUuid()))).toList());
        return dto;
    }

    private ProcessPlanBatchItemDTO item(ProcessAiDraftApplyCommand command,
                                         ProcessAiCompiledPlan candidate,
                                         ProcessConfigDraft current) {
        ProcessPlanBatchItemDTO item = new ProcessPlanBatchItemDTO();
        item.setOriginalUuid(candidate.originalUuid());
        item.setPlan(fieldMerger.merge(readPlan(current), candidate,
                command.acceptedFieldPaths()));
        return item;
    }

    private Map<String, ProcessConfigDraft> currentDrafts(
            String orderUuid, List<ProcessAiCompiledPlan> plans) {
        List<String> ids = plans.stream().map(ProcessAiCompiledPlan::originalUuid).toList();
        Map<String, ProcessConfigDraft> result = new LinkedHashMap<>();
        draftMapper.selectList(new LambdaQueryWrapper<ProcessConfigDraft>()
                        .eq(ProcessConfigDraft::getOrderUuid, orderUuid)
                        .in(ProcessConfigDraft::getOriginalUuid, ids))
                .forEach(draft -> result.put(draft.getOriginalUuid(), draft));
        return result;
    }

    private ProcessPlanDTO readPlan(ProcessConfigDraft draft) {
        if (draft == null) return null;
        try {
            JsonNode node = objectMapper.readTree(draft.getConfigJson());
            if (node.has("stages")) throw new BusinessException("链式工艺不能被AI单道方案覆盖");
            if (node.has("rewindSegments")) {
                return planMapper.fromSaveDto(objectMapper.treeToValue(node, FinishConfigSaveDTO.class));
            }
            return objectMapper.treeToValue(node, ProcessPlanDTO.class);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("现有工艺草稿无法解析，禁止AI覆盖");
        }
    }

    private void saveIntent(ProcessAiDraftApplyCommand command, String originalUuid) {
        draftMapper.update(null, new LambdaUpdateWrapper<ProcessConfigDraft>()
                .eq(ProcessConfigDraft::getOrderUuid, command.orderUuid())
                .eq(ProcessConfigDraft::getOriginalUuid, originalUuid)
                .set(ProcessConfigDraft::getAiIntentJson,
                        "{\"parseId\":\"" + command.parseId() + "\"}"));
    }

    private Map<String, ProcessAiCompiledPlan> index(List<ProcessAiCompiledPlan> plans) {
        Map<String, ProcessAiCompiledPlan> result = new LinkedHashMap<>();
        plans.forEach(plan -> result.put(plan.originalUuid(), plan));
        return result;
    }

    private void requireReady(Integer rowSort, PlanPreviewVO preview) {
        if (preview.isReady()) return;
        String rollLabel = rowSort == null ? "当前母卷" : "原纸" + rowSort;
        String details = preview.getErrors().isEmpty()
                ? "工艺字段组合无效" : String.join("；", preview.getErrors());
        throw new BusinessException(ResultCode.BAD_REQUEST, "AI_APPLIED_PLAN_INVALID",
                rollLabel + "的AI工艺方案预览未通过：" + details + "。未写入任何草稿");
    }
}
