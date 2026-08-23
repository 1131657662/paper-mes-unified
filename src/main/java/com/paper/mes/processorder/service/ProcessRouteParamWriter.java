package com.paper.mes.processorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import com.paper.mes.processorder.entity.ProcessParam;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.mapper.ProcessParamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Persists rewind geometry used by route detail, pricing, and later back-record reads. */
@Component
@RequiredArgsConstructor
public class ProcessRouteParamWriter {

    private static final int REWIND_STEP_TYPE = 2;
    private final ProcessParamMapper processParamMapper;
    private final ObjectMapper objectMapper;

    public void write(ProcessRouteContext context,
                      ProcessRoutePreviewDTO dto,
                      Map<String, ProcessStageOutput> outputsByKey) {
        if (dto.getStages() == null) return;
        for (ProcessRoutePreviewDTO.RouteStageDTO stage : dto.getStages()) {
            writeStage(context, stage, stepUuid(stage, outputsByKey), outputsByKey);
        }
    }

    private void writeStage(ProcessRouteContext context,
                            ProcessRoutePreviewDTO.RouteStageDTO stage,
                            String stepUuid,
                            Map<String, ProcessStageOutput> outputsByKey) {
        if (stage.getStepType() == null || stage.getStepType() != REWIND_STEP_TYPE
                || stage.getPlan() == null || stage.getPlan().getSegments() == null
                || stage.getPlan().getSegments().isEmpty() || stepUuid == null) return;
        List<ProcessStageOutput> outputs = stageOutputs(stage, stepUuid, outputsByKey);
        int outputIndex = 0;
        int layerSort = 1;
        for (RewindSegmentPlanDTO segment : stage.getPlan().getSegments()) {
            int repeatCount = positive(segment.getRepeatCount());
            for (int repeat = 0; repeat < repeatCount; repeat++) {
                for (RewindLayoutItemPlanDTO item : safe(segment.getLayoutItems())) {
                    if (isTrim(item)) continue;
                    int quantity = positive(item.getQuantity());
                    for (int copy = 0; copy < quantity; copy++) {
                        ProcessStageOutput output = outputIndex < outputs.size() ? outputs.get(outputIndex++) : null;
                        if (stage.getPlan().getRewindMode() != null
                                && stage.getPlan().getRewindMode() == 4
                                && item.getLayers() != null && !item.getLayers().isEmpty()) {
                            for (var layer : item.getLayers()) {
                                insert(context, stage, segment, stepUuid, layerSort++, item.getWidth(),
                                        layer.getOutDiameter(), layer.getCoreDiameter(), item,
                                        output == null ? null : output.getEstimateWeight(), outputsByKey);
                            }
                        } else {
                            insert(context, stage, segment, stepUuid, layerSort++, item.getWidth(),
                                    segment.getTargetDiameter(), segment.getFinishCoreDiameter(), item,
                                    output == null ? null : output.getEstimateWeight(), outputsByKey);
                        }
                    }
                }
            }
        }
    }

    private void insert(ProcessRouteContext context,
                         ProcessRoutePreviewDTO.RouteStageDTO stage,
                         RewindSegmentPlanDTO segment,
                         String stepUuid,
                         int layerSort,
                         Integer layerWidth,
                         Integer outDiameter,
                         Integer coreDiameter,
                         RewindLayoutItemPlanDTO item,
                         BigDecimal estimateWeight,
                         Map<String, ProcessStageOutput> outputsByKey) {
        ProcessParam param = new ProcessParam();
        param.setOrderUuid(context.order().getUuid());
        param.setOriginalUuid(context.roll().getUuid());
        param.setStepUuid(stepUuid);
        param.setParamMode(stage.getPlan().getRewindMode());
        param.setLayerSort(layerSort);
        param.setOutDiameter(outDiameter);
        param.setCoreDiameter(coreDiameter);
        param.setLayerWidth(layerWidth);
        param.setAreaValue(areaBasis(stage, segment, item, layerWidth, outDiameter, coreDiameter,
                context, outputsByKey));
        param.setAreaRatio(estimateWeight == null ? null : estimateWeight.setScale(0, RoundingMode.HALF_UP));
        param.setParamJson(json(segment, item));
        param.setRemark(stage.getPlan().getRemark());
        processParamMapper.insert(param);
    }

    private List<ProcessStageOutput> stageOutputs(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                                  String stepUuid,
                                                  Map<String, ProcessStageOutput> outputsByKey) {
        return outputsByKey.values().stream()
                .filter(output -> output != null && Objects.equals(stage.getStageLevel(), output.getStageLevel()))
                .filter(output -> Objects.equals(stepUuid, output.getStepUuid()))
                .filter(output -> !isTrimOutput(output))
                .sorted(Comparator.comparing(output -> output.getOutputSort() == null ? 0 : output.getOutputSort()))
                .toList();
    }

    private boolean isTrimOutput(ProcessStageOutput output) {
        return "修边/余料".equals(output.getRemark())
                || "修边/余料".equals(output.getPaperName())
                || "修边".equals(output.getPaperName())
                || "切边".equals(output.getPaperName())
                || "修边".equals(output.getOutputNo())
                || "切边".equals(output.getOutputNo());
    }

    private BigDecimal areaBasis(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                 RewindSegmentPlanDTO segment,
                                 RewindLayoutItemPlanDTO item,
                                 Integer layerWidth,
                                 Integer outDiameter,
                                 Integer coreDiameter,
                                 ProcessRouteContext context,
                                 Map<String, ProcessStageOutput> outputsByKey) {
        Integer mode = stage.getPlan().getRewindMode();
        BigDecimal width = BigDecimal.valueOf(Math.max(1, layerWidth == null ? 1 : layerWidth));
        if (mode == null || mode == 1 || mode == 5 || mode == 6) return width;
        BigDecimal area = mode == 4 ? crossSection(outDiameter, coreDiameter) : crossSection(segment);
        if (mode == 2 || mode == 4 || area.signum() == 0) return area.signum() == 0 ? width : area;
        BigDecimal sourceWidth = sourceWidth(stage, context, outputsByKey);
        return sourceWidth.signum() <= 0 ? area : area.multiply(width)
                .divide(sourceWidth, 12, RoundingMode.HALF_UP);
    }

    private BigDecimal sourceWidth(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                   ProcessRouteContext context,
                                   Map<String, ProcessStageOutput> outputsByKey) {
        if (stage.getInputOutputKeys() != null && !stage.getInputOutputKeys().isEmpty()) {
            for (String key : stage.getInputOutputKeys()) {
                ProcessStageOutput input = outputsByKey.get(key);
                if (input != null && input.getFinishWidth() != null && input.getFinishWidth() > 0) {
                    return BigDecimal.valueOf(input.getFinishWidth());
                }
            }
        }
        return BigDecimal.valueOf(Math.max(0, context.roll().getActualWidth() == null
                ? context.roll().getOriginalWidth() == null ? 0 : context.roll().getOriginalWidth()
                : context.roll().getActualWidth()));
    }

    private BigDecimal crossSection(RewindSegmentPlanDTO segment) {
        if (segment.getTargetDiameter() == null || segment.getFinishCoreDiameter() == null) return BigDecimal.ZERO;
        return crossSection(segment.getTargetDiameter(), segment.getFinishCoreDiameter());
    }

    private BigDecimal crossSection(Integer outDiameter, Integer coreDiameter) {
        if (outDiameter == null || coreDiameter == null) return BigDecimal.ZERO;
        return com.paper.mes.processorder.calc.RewindWeightCalculator.crossSectionArea(
                com.paper.mes.processorder.calc.RewindWeightCalculator.storedDiameterToMm(
                        BigDecimal.valueOf(outDiameter)),
                com.paper.mes.processorder.calc.RewindWeightCalculator.storedCoreDiameterToMm(
                        BigDecimal.valueOf(coreDiameter)));
    }

    private boolean isTrim(RewindLayoutItemPlanDTO item) {
        return "TRIM".equalsIgnoreCase(item.getItemType());
    }

    private int positive(Integer value) {
        return value == null ? 1 : Math.max(1, value);
    }

    private String stepUuid(ProcessRoutePreviewDTO.RouteStageDTO stage,
                            Map<String, ProcessStageOutput> outputsByKey) {
        List<String> stepUuids = outputsByKey.values().stream()
                .filter(row -> row != null && row.getStepUuid() != null
                        && Objects.equals(stage.getStageLevel(), row.getStageLevel()))
                .map(ProcessStageOutput::getStepUuid)
                .distinct()
                .toList();
        if (stepUuids.size() > 1) {
            throw new BusinessException("复卷工艺参数无法唯一绑定工序");
        }
        return stepUuids.isEmpty() ? null : stepUuids.get(0);
    }

    private String json(RewindSegmentPlanDTO segment, RewindLayoutItemPlanDTO item) {
        try {
            return objectMapper.writeValueAsString(Map.of("segment", segment, "layout", item));
        } catch (JsonProcessingException ex) {
            throw new BusinessException("复卷工艺参数保存失败");
        }
    }

    private List<RewindLayoutItemPlanDTO> safe(List<RewindLayoutItemPlanDTO> items) {
        return items == null ? List.of() : items;
    }
}
