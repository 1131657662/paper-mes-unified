package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProcessRouteExistingOutputResolver {
    private static final int OUTPUT_FINAL = 2;
    private static final int OUTPUT_CONSUMED = 2;
    private static final int OUTPUT_FINISH_CREATED = 3;
    private static final int OUTPUT_VOID = 4;
    private static final int ROLL_NO_VOID = 3;

    private final ProcessStageOutputMapper stageOutputMapper;
    private final FinishRollMapper finishRollMapper;
    private final FinishOriginalRelMapper finishOriginalRelMapper;
    private final ProcessStepMapper processStepMapper;
    public Map<String, ProcessStageOutput> resolveForPreview(ProcessRouteContext context,
                                                             ProcessRoutePreviewDTO dto) {
        return resolve(context, dto, false);
    }
    public Map<String, ProcessStageOutput> resolveForSave(ProcessRouteContext context,
                                                          ProcessRoutePreviewDTO dto) {
        return resolve(context, dto, true);
    }
    private Map<String, ProcessStageOutput> resolve(ProcessRouteContext context,
                                                    ProcessRoutePreviewDTO dto,
                                                    boolean persistMissing) {
        List<String> keys = firstStageInputKeys(dto);
        List<ProcessStageOutput> existingOutputs = stageOutputs(context);
        Map<String, ProcessStageOutput> result = new LinkedHashMap<>();
        for (String key : keys) {
            ProcessStageOutput output = findStageOutput(existingOutputs, key);
            if (output == null) {
                FinishRoll finish = findFinish(context, key);
                output = finish == null ? null : findStageOutputByFinish(existingOutputs, finish.getUuid());
                if (output == null) {
                    output = sourceFromFinish(context, key, finish, persistMissing);
                }
            }
            validateUsable(output, key);
            result.put(key, output);
        }
        return result;
    }
    private List<String> firstStageInputKeys(ProcessRoutePreviewDTO dto) {
        if (dto.getStages() == null || dto.getStages().isEmpty()) {
            throw new BusinessException("追加工艺不能为空");
        }
        List<String> keys = dto.getStages().get(0).getInputOutputKeys();
        if (keys == null || keys.isEmpty()) {
            throw new BusinessException("追加工艺必须选择要继续加工的上游产物");
        }
        Set<String> seen = new HashSet<>();
        for (String key : keys) {
            if (!StringUtils.hasText(key)) {
                throw new BusinessException("追加工艺来源产物不能为空");
            }
            if (!seen.add(key)) {
                throw new BusinessException("追加工艺来源产物重复：" + key);
            }
        }
        return keys;
    }
    private List<ProcessStageOutput> stageOutputs(ProcessRouteContext context) {
        return stageOutputMapper.selectList(new LambdaQueryWrapper<ProcessStageOutput>()
                .eq(ProcessStageOutput::getOrderUuid, context.order().getUuid())
                .eq(ProcessStageOutput::getOriginalUuid, context.roll().getUuid()));
    }
    private ProcessStageOutput findStageOutput(List<ProcessStageOutput> outputs, String key) {
        for (ProcessStageOutput output : outputs) {
            if (matches(output, key)) {
                return output;
            }
        }
        return null;
    }
    private boolean matches(ProcessStageOutput output, String key) {
        return key.equals(output.getUuid())
                || key.equals(output.getOutputNo())
                || key.equals(output.getFinishRollUuid())
                || key.equals("F:" + output.getFinishRollUuid());
    }
    private ProcessStageOutput findStageOutputByFinish(List<ProcessStageOutput> outputs, String finishUuid) {
        for (ProcessStageOutput output : outputs) {
            if (finishUuid.equals(output.getFinishRollUuid())) {
                return output;
            }
        }
        return null;
    }
    private ProcessStageOutput sourceFromFinish(ProcessRouteContext context, String key, FinishRoll finish,
                                                boolean persistMissing) {
        if (finish == null) {
            throw new BusinessException("未找到可继续加工的阶段产物：" + key);
        }
        requireFinishBelongsToRoll(context, finish);
        ProcessStageOutput output = buildSourceOutput(context, key, finish);
        if (persistMissing) {
            stageOutputMapper.insert(output);
        }
        return output;
    }
    private FinishRoll findFinish(ProcessRouteContext context, String key) {
        String normalized = normalizeFinishKey(key);
        List<FinishRoll> finishes = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, context.order().getUuid()));
        for (FinishRoll finish : finishes) {
            if (normalized.equals(finish.getUuid()) || key.equals(finish.getFinishRollNo())) {
                return finish;
            }
        }
        return null;
    }
    private String normalizeFinishKey(String key) {
        return key != null && key.startsWith("F:") ? key.substring(2) : key;
    }
    private void requireFinishBelongsToRoll(ProcessRouteContext context, FinishRoll finish) {
        FinishOriginalRel rel = finishOriginalRelMapper.selectOne(new LambdaQueryWrapper<FinishOriginalRel>()
                .eq(FinishOriginalRel::getOrderUuid, context.order().getUuid())
                .eq(FinishOriginalRel::getOriginalUuid, context.roll().getUuid())
                .eq(FinishOriginalRel::getFinishUuid, finish.getUuid())
                .last("LIMIT 1"));
        if (rel == null) {
            throw new BusinessException("成品卷不属于当前来源母卷，不能作为后续工艺来源");
        }
    }
    private ProcessStageOutput buildSourceOutput(ProcessRouteContext context, String key, FinishRoll finish) {
        ProcessStep step = latestStep(context);
        ProcessStageOutput output = new ProcessStageOutput();
        output.setOrderUuid(context.order().getUuid());
        output.setOriginalUuid(context.roll().getUuid());
        output.setStepUuid(step == null ? "legacy-finish" : step.getUuid());
        output.setStageLevel(step == null || step.getStageLevel() == null ? 1 : step.getStageLevel());
        output.setOutputSort(finish.getRowSort() == null ? 1 : finish.getRowSort());
        output.setOutputType(OUTPUT_FINAL);
        output.setOutputStatus(OUTPUT_FINISH_CREATED);
        output.setOutputNo(key);
        output.setFinishRollUuid(finish.getUuid());
        output.setPaperName(finish.getPaperName());
        output.setGramWeight(finish.getGramWeight());
        output.setFinishWidth(finish.getFinishWidth());
        output.setFinishDiameter(finish.getFinishDiameter());
        output.setFinishCoreDiameter(finish.getFinishCoreDiameter());
        output.setEstimateWeight(finish.getEstimateWeight());
        output.setActualWeight(finish.getActualWeight());
        output.setSourceStepType(step == null ? null : step.getStepType());
        output.setSourceSummary("既有成品转后续工艺来源");
        output.setRemark(finish.getFinishRollNo());
        return output;
    }
    private ProcessStep latestStep(ProcessRouteContext context) {
        return processStepMapper.selectOne(new LambdaQueryWrapper<ProcessStep>()
                .eq(ProcessStep::getOrderUuid, context.order().getUuid())
                .eq(ProcessStep::getOriginalUuid, context.roll().getUuid())
                .orderByDesc(ProcessStep::getStepSort)
                .last("LIMIT 1"));
    }
    private void validateUsable(ProcessStageOutput output, String key) {
        if (output.getOutputStatus() != null && output.getOutputStatus() == OUTPUT_CONSUMED) {
            throw new BusinessException("该产物已经进入下道工艺，不能重复加工：" + key);
        }
        if (output.getOutputStatus() != null && output.getOutputStatus() == OUTPUT_VOID) {
            throw new BusinessException("该产物已作废，不能继续加工：" + key);
        }
        if (!StringUtils.hasText(output.getFinishRollUuid())) {
            return;
        }
        FinishRoll finish = finishRollMapper.selectById(output.getFinishRollUuid());
        if (finish != null && finish.getActualWeight() != null) {
            throw new BusinessException("已有回录实重的成品不能再追加后续工艺：" + key);
        }
        if (finish != null && finish.getRollNoStatus() != null && finish.getRollNoStatus() == ROLL_NO_VOID) {
            throw new BusinessException("已作废的成品号不能作为后续工艺来源：" + key);
        }
    }
}
