package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
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
    private final ProcessRouteExistingOutputLoader loader;
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
        List<ProcessStageOutput> existingOutputs = loader.stageOutputs(context);
        List<FinishRoll> finishes = loader.finishRolls(context);
        Map<String, FinishRoll> finishesByKey = indexFinishes(finishes);
        Map<String, ProcessStageOutput> result = new LinkedHashMap<>();
        Map<String, FinishRoll> missing = new LinkedHashMap<>();
        for (String key : keys) {
            ProcessStageOutput output = findStageOutput(existingOutputs, key);
            FinishRoll finish = null;
            if (output == null) {
                finish = finishesByKey.get(key);
                output = finish == null ? null : findStageOutputByFinish(existingOutputs, finish.getUuid());
            }
            requireResolvable(output, finish, key);
            if (output == null) {
                missing.put(key, finish);
            } else {
                validateUsable(output, key, finishesByKey);
            }
            result.put(key, output);
        }
        populateMissingSources(context, missing, result, persistMissing);
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
    private Map<String, FinishRoll> indexFinishes(List<FinishRoll> finishes) {
        Map<String, FinishRoll> result = new LinkedHashMap<>();
        for (FinishRoll finish : finishes) {
            if (StringUtils.hasText(finish.getUuid())) {
                result.putIfAbsent(finish.getUuid(), finish);
                result.putIfAbsent("F:" + finish.getUuid(), finish);
            }
            if (StringUtils.hasText(finish.getFinishRollNo())) {
                result.putIfAbsent(finish.getFinishRollNo(), finish);
            }
        }
        return result;
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
    private void requireResolvable(ProcessStageOutput output, FinishRoll finish, String key) {
        if (output == null && finish == null) {
            throw new BusinessException("未找到可继续加工的阶段产物：" + key);
        }
    }
    private void populateMissingSources(ProcessRouteContext context, Map<String, FinishRoll> missing,
                                        Map<String, ProcessStageOutput> result, boolean persistMissing) {
        if (missing.isEmpty()) {
            return;
        }
        Set<String> relatedFinishUuids = loader.relatedFinishUuids(context, missing.values());
        missing.values().forEach(finish -> requireFinishBelongsToRoll(relatedFinishUuids, finish));
        missing.forEach(this::validateFinishUsable);
        ProcessStep step = loader.latestStep(context);
        for (Map.Entry<String, FinishRoll> entry : missing.entrySet()) {
            ProcessStageOutput output = buildSourceOutput(context, entry.getKey(), entry.getValue(), step);
            if (persistMissing) {
                stageOutputMapper.insert(output);
            }
            result.put(entry.getKey(), output);
        }
    }
    private void requireFinishBelongsToRoll(Set<String> relatedFinishUuids, FinishRoll finish) {
        if (!relatedFinishUuids.contains(finish.getUuid())) {
            throw new BusinessException("成品卷不属于当前来源母卷，不能作为后续工艺来源");
        }
    }
    private ProcessStageOutput buildSourceOutput(ProcessRouteContext context, String key, FinishRoll finish,
                                                 ProcessStep step) {
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
    private void validateUsable(ProcessStageOutput output, String key, Map<String, FinishRoll> finishesByKey) {
        if (output.getOutputStatus() != null && output.getOutputStatus() == OUTPUT_CONSUMED) {
            throw new BusinessException("该产物已经进入下道工艺，不能重复加工：" + key);
        }
        if (output.getOutputStatus() != null && output.getOutputStatus() == OUTPUT_VOID) {
            throw new BusinessException("该产物已作废，不能继续加工：" + key);
        }
        if (!StringUtils.hasText(output.getFinishRollUuid())) {
            return;
        }
        FinishRoll finish = finishesByKey.get(output.getFinishRollUuid());
        validateFinishUsable(key, finish);
    }
    private void validateFinishUsable(String key, FinishRoll finish) {
        if (finish != null && finish.getActualWeight() != null) {
            throw new BusinessException("已有回录实重的成品不能再追加后续工艺：" + key);
        }
        if (finish != null && finish.getRollNoStatus() != null && finish.getRollNoStatus() == ROLL_NO_VOID) {
            throw new BusinessException("已作废的成品号不能作为后续工艺来源：" + key);
        }
        if (FinishRollStatusPolicy.isScrapped(finish)) {
            throw new BusinessException("已报废成品不能作为后续工艺来源：" + key);
        }
    }
}
