package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.ProcessStageInputRel;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStageInputRelMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProcessRouteStepWriter {

    private static final int INPUT_ORIGINAL = 1;
    private static final int INPUT_STAGE_OUTPUT = 2;
    private static final int OUTPUT_INTERMEDIATE = 1;
    private static final int OUTPUT_FINAL = 2;
    private static final int OUTPUT_PLANNED = 1;
    private static final int OUTPUT_CONSUMED = 2;

    private final ProcessStepMapper processStepMapper;
    private final ProcessStageOutputMapper stageOutputMapper;
    private final ProcessStageInputRelMapper stageInputRelMapper;

    public Map<String, ProcessStageOutput> write(ProcessRouteContext context,
                                                 ProcessRoutePreviewDTO dto,
                                                 ProcessRoutePreviewVO preview) {
        return writeWith(context, dto, preview, new LinkedHashMap<>(), 1, true);
    }

    public Map<String, ProcessStageOutput> writeAppend(ProcessRouteContext context,
                                                       ProcessRoutePreviewDTO dto,
                                                       ProcessRoutePreviewVO preview,
                                                       Map<String, ProcessStageOutput> initialOutputs,
                                                       int startStepSort) {
        return writeWith(context, dto, preview, new LinkedHashMap<>(initialOutputs), startStepSort, false);
    }

    private Map<String, ProcessStageOutput> writeWith(ProcessRouteContext context,
                                                      ProcessRoutePreviewDTO dto,
                                                      ProcessRoutePreviewVO preview,
                                                      Map<String, ProcessStageOutput> outputsByKey,
                                                      int startStepSort,
                                                      boolean firstIsMain) {
        for (int i = 0; i < dto.getStages().size(); i++) {
            ProcessRoutePreviewDTO.RouteStageDTO stage = dto.getStages().get(i);
            ProcessStep step = buildStep(context, stage, preview.getStages().get(i), outputsByKey,
                    startStepSort + i, firstIsMain && i == 0);
            processStepMapper.insert(step);
            appendStageInputs(context, stage, step, outputsByKey);
            appendStageOutputs(context, stage, preview, step, outputsByKey);
        }
        return outputsByKey;
    }

    private ProcessStep buildStep(ProcessRouteContext context,
                                  ProcessRoutePreviewDTO.RouteStageDTO stage,
                                  ProcessRoutePreviewVO.RouteStageLineVO line,
                                  Map<String, ProcessStageOutput> outputsByKey,
                                  int stepSort,
                                  boolean isMain) {
        ProcessStageOutput parent = firstInputOutput(stage, outputsByKey);
        ProcessStep step = new ProcessStep();
        step.setOrderUuid(context.order().getUuid());
        step.setOriginalUuid(context.roll().getUuid());
        step.setInputType(stage.getStageLevel() == null || stage.getStageLevel() <= 1 ? INPUT_ORIGINAL : INPUT_STAGE_OUTPUT);
        step.setInputOutputUuid(parent == null ? null : parent.getUuid());
        step.setParentStepUuid(parent == null ? null : parent.getStepUuid());
        step.setStageLevel(stage.getStageLevel());
        step.setStepSort(stepSort);
        step.setStepType(stage.getStepType());
        step.setStepName(StringUtils.hasText(stage.getStepName()) ? stage.getStepName() : stepName(stage.getStepType()));
        step.setIsMain(isMain ? 1 : 0);
        step.setKnifeCount(stage.getKnifeCount());
        step.setProcessWeight(line.getProcessWeight());
        step.setUnitPrice(stage.getUnitPrice());
        step.setStepAmount(line.getStepAmount());
        return step;
    }

    private void appendStageInputs(ProcessRouteContext context,
                                   ProcessRoutePreviewDTO.RouteStageDTO stage,
                                   ProcessStep step,
                                   Map<String, ProcessStageOutput> outputsByKey) {
        if (stage.getInputOutputKeys() == null || stage.getInputOutputKeys().isEmpty()) {
            return;
        }
        int sort = 1;
        for (String inputKey : stage.getInputOutputKeys()) {
            ProcessStageOutput input = outputsByKey.get(inputKey);
            if (input == null) {
                continue;
            }
            ProcessStageInputRel rel = new ProcessStageInputRel();
            rel.setOrderUuid(context.order().getUuid());
            rel.setOriginalUuid(context.roll().getUuid());
            rel.setStepUuid(step.getUuid());
            rel.setInputOutputUuid(input.getUuid());
            rel.setSourceStepUuid(input.getStepUuid());
            rel.setInputSort(sort++);
            rel.setStageLevel(stage.getStageLevel());
            stageInputRelMapper.insert(rel);
        }
    }

    private void appendStageOutputs(ProcessRouteContext context,
                                    ProcessRoutePreviewDTO.RouteStageDTO stage,
                                    ProcessRoutePreviewVO preview,
                                    ProcessStep step,
                                    Map<String, ProcessStageOutput> outputsByKey) {
        for (ProcessRoutePreviewVO.RouteOutputVO output : preview.getOutputs()) {
            if (!stage.getStageLevel().equals(output.getStageLevel())) {
                continue;
            }
            ProcessStageOutput row = buildOutput(context, stage, step, output, outputsByKey);
            stageOutputMapper.insert(row);
            outputsByKey.put(output.getOutputKey(), row);
        }
    }

    private ProcessStageOutput buildOutput(ProcessRouteContext context,
                                           ProcessRoutePreviewDTO.RouteStageDTO stage,
                                           ProcessStep step,
                                           ProcessRoutePreviewVO.RouteOutputVO output,
                                           Map<String, ProcessStageOutput> outputsByKey) {
        ProcessStageOutput row = new ProcessStageOutput();
        row.setOrderUuid(context.order().getUuid());
        row.setOriginalUuid(context.roll().getUuid());
        row.setStepUuid(step.getUuid());
        row.setParentOutputUuid(parentOutputUuid(stage, outputsByKey));
        row.setStageLevel(output.getStageLevel());
        row.setOutputSort(output.getOutputSort());
        row.setOutputType(Boolean.TRUE.equals(output.getConsumedByNextStage()) ? OUTPUT_INTERMEDIATE : OUTPUT_FINAL);
        row.setOutputStatus(Boolean.TRUE.equals(output.getConsumedByNextStage()) ? OUTPUT_CONSUMED : OUTPUT_PLANNED);
        row.setOutputNo(output.getOutputKey());
        row.setPaperName(output.getPaperName());
        row.setGramWeight(output.getGramWeight());
        row.setFinishWidth(output.getFinishWidth());
        row.setFinishDiameter(output.getFinishDiameter());
        row.setFinishCoreDiameter(output.getFinishCoreDiameter());
        row.setEstimateWeight(output.getEstimateWeight());
        row.setSourceStepType(stage.getStepType());
        row.setSourceSummary(step.getStepName());
        row.setRemark(output.getRemark());
        return row;
    }

    private ProcessStageOutput firstInputOutput(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                                Map<String, ProcessStageOutput> outputsByKey) {
        if (stage.getInputOutputKeys() == null || stage.getInputOutputKeys().isEmpty()) {
            return null;
        }
        return outputsByKey.get(stage.getInputOutputKeys().get(0));
    }

    private String parentOutputUuid(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                    Map<String, ProcessStageOutput> outputsByKey) {
        ProcessStageOutput output = firstInputOutput(stage, outputsByKey);
        return output == null ? null : output.getUuid();
    }

    private String stepName(Integer stepType) {
        if (stepType != null && stepType == 1) return "锯纸";
        if (stepType != null && stepType == 2) return "复卷";
        return null;
    }
}
