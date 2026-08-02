package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProcessRoutePreviewer {
    private static final int IS_REMAIN_YES = 1;
    private static final int IS_REMAIN_NO = 0;

    private final ProcessRouteCatalogPolicy routeCatalogPolicy;

    public ProcessRoutePreviewVO preview(OriginalRoll roll, ProcessRoutePreviewDTO dto) {
        return preview(roll, dto, Map.of());
    }
    public ProcessRoutePreviewVO previewFromExistingOutputs(OriginalRoll roll,
                                                            Map<String, ProcessStageOutput> sourceOutputsByKey,
                                                            ProcessRoutePreviewDTO dto) {
        Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey = new HashMap<>();
        for (Map.Entry<String, ProcessStageOutput> entry : sourceOutputsByKey.entrySet()) {
            outputsByKey.put(entry.getKey(), sourceOutputLine(entry.getKey(), entry.getValue()));
        }
        return preview(roll, dto, outputsByKey);
    }
    private ProcessRoutePreviewVO preview(OriginalRoll roll, ProcessRoutePreviewDTO dto,
                                          Map<String, ProcessRoutePreviewVO.RouteOutputVO> initialOutputs) {
        ProcessRouteQuantityValidator.requireWithinLimit(dto);
        validateStageSequence(dto.getStages(), initialOutputs);
        routeCatalogPolicy.validate(dto.getStages());
        Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey = new HashMap<>(initialOutputs);
        Set<String> consumedKeys = consumedOutputKeys(dto.getStages());
        Set<String> usedInputKeys = new HashSet<>();
        List<ProcessRoutePreviewVO.RouteStageLineVO> stageLines = new ArrayList<>();
        List<ProcessRoutePreviewVO.RouteOutputVO> outputLines = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ProcessRoutePreviewDTO.RouteStageDTO stage : dto.getStages()) {
            validateStageInputs(stage, outputsByKey, usedInputKeys, initialOutputs.keySet());
            ProcessRoutePreviewValidator.validateStageWeight(roll, stage, outputsByKey);
            BigDecimal processWeight = resolveProcessWeight(roll, stage, outputsByKey);
            BigDecimal amount = FeeCalculator.stepAmount(stage.getStepType(), stage.getKnifeCount(), processWeight, stage.getUnitPrice());
            totalAmount = totalAmount.add(amount);
            stageLines.add(stageLine(stage, processWeight, amount));
            appendOutputs(stage, roll, consumedKeys, outputsByKey, outputLines);
        }
        ProcessRoutePreviewVO vo = new ProcessRoutePreviewVO();
        vo.setOriginalUuid(roll.getUuid());
        vo.setStages(stageLines);
        vo.setOutputs(outputLines);
        vo.setTotalAmount(totalAmount);
        return vo;
    }
    private ProcessRoutePreviewVO.RouteOutputVO sourceOutputLine(String outputKey, ProcessStageOutput output) {
        ProcessRoutePreviewVO.RouteOutputVO line = new ProcessRoutePreviewVO.RouteOutputVO();
        line.setOutputKey(outputKey);
        line.setStageLevel(output.getStageLevel());
        line.setOutputSort(output.getOutputSort());
        line.setOutputType(output.getOutputType());
        line.setConsumedByNextStage(false);
        line.setIsRemain(isTrimOutput(output) ? IS_REMAIN_YES : IS_REMAIN_NO);
        line.setPaperName(output.getPaperName());
        line.setGramWeight(output.getGramWeight());
        line.setFinishWidth(output.getFinishWidth());
        line.setFinishDiameter(output.getFinishDiameter());
        line.setFinishCoreDiameter(output.getFinishCoreDiameter());
        line.setEstimateWeight(output.getEstimateWeight());
        line.setRemark(output.getRemark());
        return line;
    }
    private void validateStageInputs(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                     Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey,
                                     Set<String> usedInputKeys,
                                     Set<String> initialOutputKeys) {
        if (stage.getStageLevel() == null || stage.getStageLevel() <= 1) {
            if (stage.getInputOutputKeys() != null && !stage.getInputOutputKeys().isEmpty()) {
                throw new BusinessException("首段工艺不能引用前置阶段产出");
            }
            return;
        }
        if (stage.getInputOutputKeys() == null || stage.getInputOutputKeys().isEmpty()) {
            throw new BusinessException("后续工艺必须选择上一阶段产出");
        }
        Set<String> seen = new HashSet<>();
        for (String key : stage.getInputOutputKeys()) {
            if (key == null || key.isBlank()) {
                throw new BusinessException("后续工艺引用的阶段产出不能为空");
            }
            if (!seen.add(key)) {
                throw new BusinessException("后续工艺重复引用阶段产出：" + key);
            }
            if (!outputsByKey.containsKey(key)) {
                throw new BusinessException("后续工艺引用了不存在的阶段产出：" + key);
            }
            Integer sourceLevel = outputsByKey.get(key).getStageLevel();
            if (sourceLevel == null && !initialOutputKeys.contains(key)) {
                throw new BusinessException("阶段产出缺少阶段编号：" + key);
            }
            if (sourceLevel != null && sourceLevel != stage.getStageLevel() - 1) {
                throw new BusinessException("后续工艺只能引用紧邻上一阶段产出：" + key);
            }
            if (isRemainOutput(outputsByKey.get(key))) {
                throw new BusinessException("修边/余料不能作为后续工艺来源：" + key);
            }
            if (!usedInputKeys.add(key)) {
                throw new BusinessException("阶段产出不能重复作为后续工艺来源：" + key);
            }
        }
    }

    private void validateStageSequence(List<ProcessRoutePreviewDTO.RouteStageDTO> stages,
                                       Map<String, ProcessRoutePreviewVO.RouteOutputVO> initialOutputs) {
        if (stages == null || stages.isEmpty()) {
            throw new BusinessException("工艺阶段不能为空");
        }
        int previous = 0;
        for (int i = 0; i < stages.size(); i++) {
            ProcessRoutePreviewDTO.RouteStageDTO stage = stages.get(i);
            Integer level = stage.getStageLevel();
            if (level == null || level <= 0) {
                throw new BusinessException("阶段编号必须为正数");
            }
            if (i > 0 && level != previous + 1) {
                throw new BusinessException("工艺阶段必须按连续编号顺序提交");
            }
            validateStageNumbers(stage);
            previous = level;
        }
        Integer firstLevel = stages.get(0).getStageLevel();
        if (firstLevel > 1 && initialOutputs.isEmpty()) {
            throw new BusinessException("非首段工艺缺少上一阶段产出");
        }
    }

    private void validateStageNumbers(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        if (stage.getKnifeCount() != null && stage.getKnifeCount() <= 0) {
            throw new BusinessException("刀数必须大于0");
        }
        if (stage.getProcessWeight() != null && stage.getProcessWeight().signum() <= 0) {
            throw new BusinessException("加工重量必须大于0");
        }
        if (stage.getUnitPrice() != null && stage.getUnitPrice().signum() < 0) {
            throw new BusinessException("工序单价不能为负数");
        }
        if (stage.getOutputs() == null || stage.getOutputs().isEmpty()) {
            throw new BusinessException("每道工序至少需要一个阶段产出");
        }
        for (ProcessRoutePreviewDTO.RouteOutputDTO output : stage.getOutputs()) {
            if (output.getCount() != null && output.getCount() <= 0) {
                throw new BusinessException("阶段产出数量必须大于0");
            }
            if (output.getEstimateWeight() != null && output.getEstimateWeight().signum() < 0) {
                throw new BusinessException("阶段产出重量不能为负数");
            }
            if (output.getFinishWidth() != null && output.getFinishWidth() <= 0) {
                throw new BusinessException("成品门幅必须大于0");
            }
            if (output.getFinishDiameter() != null && output.getFinishDiameter() <= 0) {
                throw new BusinessException("成品直径必须大于0");
            }
            if (output.getFinishCoreDiameter() != null && output.getFinishCoreDiameter() <= 0) {
                throw new BusinessException("纸芯直径必须大于0");
            }
        }
    }
    private Set<String> consumedOutputKeys(List<ProcessRoutePreviewDTO.RouteStageDTO> stages) {
        Set<String> keys = new HashSet<>();
        for (ProcessRoutePreviewDTO.RouteStageDTO stage : stages) {
            if (stage.getInputOutputKeys() != null) {
                keys.addAll(stage.getInputOutputKeys());
            }
        }
        return keys;
    }
    private BigDecimal resolveProcessWeight(OriginalRoll roll, ProcessRoutePreviewDTO.RouteStageDTO stage,
                                            Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey) {
        if (stage.getStepType() == null || stage.getStepType() != FeeCalculator.STEP_TYPE_REWIND) {
            return null;
        }
        if (stage.getProcessWeight() != null) {
            return stage.getProcessWeight();
        }
        if (stage.getInputOutputKeys() == null || stage.getInputOutputKeys().isEmpty()) {
            return originalWeightTon(roll);
        }
        return selectedOutputWeightTon(stage.getInputOutputKeys(), outputsByKey);
    }
    private BigDecimal originalWeightTon(OriginalRoll roll) {
        return ProcessRoutePreviewValidator.originalWeight(roll)
                .divide(FeeCalculator.TON_DIVISOR, 3, RoundingMode.HALF_UP);
    }
    private BigDecimal selectedOutputWeightTon(List<String> keys,
                                               Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey) {
        BigDecimal total = BigDecimal.ZERO;
        for (String key : keys) {
            ProcessRoutePreviewVO.RouteOutputVO output = outputsByKey.get(key);
            if (output == null) {
                throw new BusinessException("下道工艺引用了不存在的阶段产出：" + key);
            }
            total = total.add(output.getEstimateWeight() == null ? BigDecimal.ZERO : output.getEstimateWeight());
        }
        return total.divide(FeeCalculator.TON_DIVISOR, 3, RoundingMode.HALF_UP);
    }

    private ProcessRoutePreviewVO.RouteStageLineVO stageLine(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                                            BigDecimal processWeight, BigDecimal amount) {
        ProcessRoutePreviewVO.RouteStageLineVO line = new ProcessRoutePreviewVO.RouteStageLineVO();
        line.setStageLevel(stage.getStageLevel());
        line.setStepType(stage.getStepType());
        line.setStepName(stage.getStepName());
        line.setMachineUuid(resolveMachineUuid(stage));
        line.setInputOutputKeys(stage.getInputOutputKeys());
        line.setKnifeCount(stage.getKnifeCount());
        line.setProcessWeight(processWeight);
        line.setUnitPrice(stage.getUnitPrice());
        line.setStepAmount(amount);
        return line;
    }

    private String resolveMachineUuid(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        if (stage.getMachineUuid() != null && !stage.getMachineUuid().isBlank()) {
            return stage.getMachineUuid();
        }
        return stage.getPlan() == null ? null : stage.getPlan().getMachineUuid();
    }

    private void appendOutputs(ProcessRoutePreviewDTO.RouteStageDTO stage, OriginalRoll roll, Set<String> consumedKeys,
                               Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey,
                               List<ProcessRoutePreviewVO.RouteOutputVO> outputLines) {
        List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs = stage.getOutputs() == null ? List.of() : stage.getOutputs();
        int sort = 1;
        for (ProcessRoutePreviewDTO.RouteOutputDTO output : outputs) {
            int count = output.getCount() == null ? 1 : output.getCount();
            for (int i = 0; i < count; i++) {
                ProcessRoutePreviewVO.RouteOutputVO line = outputLine(stage, roll, output, consumedKeys, sort, i);
                if (outputsByKey.containsKey(line.getOutputKey())) {
                    throw new BusinessException("阶段产物编号重复：" + line.getOutputKey());
                }
                outputsByKey.put(line.getOutputKey(), line);
                outputLines.add(line);
                sort++;
            }
        }
    }

    private ProcessRoutePreviewVO.RouteOutputVO outputLine(ProcessRoutePreviewDTO.RouteStageDTO stage, OriginalRoll roll,
                                                          ProcessRoutePreviewDTO.RouteOutputDTO output,
                                                          Set<String> consumedKeys, int sort, int index) {
        String key = outputKey(stage, output, sort, index);
        ProcessRoutePreviewVO.RouteOutputVO line = new ProcessRoutePreviewVO.RouteOutputVO();
        line.setOutputKey(key);
        line.setStageLevel(stage.getStageLevel());
        line.setOutputSort(sort);
        line.setConsumedByNextStage(consumedKeys.contains(key));
        line.setOutputType(line.getConsumedByNextStage() ? 1 : output.getOutputType() == null ? 2 : output.getOutputType());
        line.setIsRemain(output.getIsRemain() != null && output.getIsRemain() == IS_REMAIN_YES ? IS_REMAIN_YES : IS_REMAIN_NO);
        line.setPaperName(output.getPaperName() == null ? roll.getPaperName() : output.getPaperName());
        line.setGramWeight(output.getGramWeight() == null ? roll.getGramWeight() : output.getGramWeight());
        line.setFinishWidth(output.getFinishWidth());
        line.setFinishDiameter(output.getFinishDiameter());
        line.setFinishCoreDiameter(output.getFinishCoreDiameter());
        line.setEstimateWeight(output.getEstimateWeight());
        line.setRemark(output.getRemark());
        return line;
    }

    private String outputKey(ProcessRoutePreviewDTO.RouteStageDTO stage, ProcessRoutePreviewDTO.RouteOutputDTO output,
                             int sort, int index) {
        if (output.getOutputKey() == null || output.getOutputKey().isBlank()) {
            return "S" + stage.getStageLevel() + "-O" + sort;
        }
        return index == 0 ? output.getOutputKey() : output.getOutputKey() + "-" + (index + 1);
    }

    private boolean isRemainOutput(ProcessRoutePreviewVO.RouteOutputVO output) {
        return output != null && output.getIsRemain() != null && output.getIsRemain() == IS_REMAIN_YES;
    }

    private boolean isTrimOutput(ProcessStageOutput output) {
        return "修边/余料".equals(output.getRemark())
                || "修边/余料".equals(output.getPaperName())
                || "修边".equals(output.getPaperName())
                || "切边".equals(output.getPaperName())
                || "修边".equals(output.getOutputNo())
                || "切边".equals(output.getOutputNo());
    }
}
