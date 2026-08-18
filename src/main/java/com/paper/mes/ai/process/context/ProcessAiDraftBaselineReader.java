package com.paper.mes.ai.process.context;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import com.paper.mes.processorder.service.ProcessPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class ProcessAiDraftBaselineReader {

    private final ProcessConfigDraftMapper draftMapper;
    private final ObjectMapper objectMapper;
    private final ProcessPlanMapper planMapper;

    List<ProcessAiBaselinePlan> read(String orderUuid, List<ProcessAiRollContext> rolls) {
        Map<String, ProcessConfigDraft> drafts = draftMapper.selectList(
                        new LambdaQueryWrapper<ProcessConfigDraft>()
                                .eq(ProcessConfigDraft::getOrderUuid, orderUuid))
                .stream().collect(Collectors.toMap(
                        ProcessConfigDraft::getOriginalUuid, Function.identity()));
        return rolls.stream().map(roll -> baseline(roll, drafts.get(roll.originalUuid()))).toList();
    }

    private ProcessAiBaselinePlan baseline(ProcessAiRollContext roll,
                                           ProcessConfigDraft draft) {
        DraftPlan parsed = readPlan(draft);
        return new ProcessAiBaselinePlan(
                roll.shortRef(), roll.originalUuid(), roll.processMode(), roll.mainStepType(),
                parsed.route(), parsed.plan());
    }

    private DraftPlan readPlan(ProcessConfigDraft draft) {
        if (draft == null || draft.getConfigJson() == null || draft.getConfigJson().isBlank()) {
            return new DraftPlan(false, null);
        }
        try {
            JsonNode node = objectMapper.readTree(draft.getConfigJson());
            if (node.has("stages")) return new DraftPlan(true, null);
            if (node.has("rewindSegments")) {
                FinishConfigSaveDTO legacy = objectMapper.treeToValue(node, FinishConfigSaveDTO.class);
                return new DraftPlan(false, planMapper.fromSaveDto(legacy));
            }
            return new DraftPlan(false, objectMapper.treeToValue(node, ProcessPlanDTO.class));
        } catch (Exception exception) {
            throw new BusinessException("AI解析无法读取当前工艺草稿，请先人工检查草稿配置");
        }
    }

    private record DraftPlan(boolean route, ProcessPlanDTO plan) {
    }
}
