package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStageInputRel;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessStageInputRelMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ProcessRouteFinishWriter {

    private static final int ROLL_NO_PRE = 1;
    private static final int FORMAL_FINISH = 0;
    private static final int IS_REMAIN_NO = 0;
    private static final int IS_REMAIN_YES = 1;
    private static final int FINISH_STATUS_PENDING = 1;
    private static final int OUTPUT_FINISH_CREATED = 3;

    private final FinishRollMapper finishRollMapper;
    private final FinishOriginalRelMapper finishOriginalRelMapper;
    private final ProcessStageOutputMapper stageOutputMapper;
    private final RollNoSequenceService rollNoSequenceService;
    private final ProcessStageInputRelMapper stageInputRelMapper;

    public ProcessRouteFinishWriter(FinishRollMapper finishRollMapper,
                                    FinishOriginalRelMapper finishOriginalRelMapper,
                                    ProcessStageOutputMapper stageOutputMapper,
                                    RollNoSequenceService rollNoSequenceService) {
        this(finishRollMapper, finishOriginalRelMapper, stageOutputMapper, rollNoSequenceService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ProcessRouteFinishWriter(FinishRollMapper finishRollMapper,
                                    FinishOriginalRelMapper finishOriginalRelMapper,
                                    ProcessStageOutputMapper stageOutputMapper,
                                    RollNoSequenceService rollNoSequenceService,
                                    ProcessStageInputRelMapper stageInputRelMapper) {
        this.finishRollMapper = finishRollMapper;
        this.finishOriginalRelMapper = finishOriginalRelMapper;
        this.stageOutputMapper = stageOutputMapper;
        this.rollNoSequenceService = rollNoSequenceService;
        this.stageInputRelMapper = stageInputRelMapper;
    }

    public void createFinalFinishes(ProcessRouteContext context,
                                    ProcessRoutePreviewVO preview,
                                    Map<String, ProcessStageOutput> outputsByKey) {
        int rowSort = nextFinishRowSort(context.order().getUuid());
        SourceTrace trace = sourceTrace(context, outputsByKey);
        for (ProcessRoutePreviewVO.RouteOutputVO output : preview.getOutputs()) {
            if (Boolean.TRUE.equals(output.getConsumedByNextStage())) {
                continue;
            }
            FinishRoll finish = buildFinish(context, output, rowSort++);
            allocAndInsertFinish(finish);
            saveRels(context, finish, output.getEstimateWeight(), outputsByKey.get(output.getOutputKey()), trace);
            markOutputFinishCreated(outputsByKey.get(output.getOutputKey()), finish);
        }
    }

    private FinishRoll buildFinish(ProcessRouteContext context,
                                   ProcessRoutePreviewVO.RouteOutputVO output,
                                   int rowSort) {
        FinishRoll finish = new FinishRoll();
        finish.setOrderUuid(context.order().getUuid());
        finish.setRowSort(rowSort);
        finish.setRollNoStatus(ROLL_NO_PRE);
        finish.setIsSpare(FORMAL_FINISH);
        finish.setIsRemain(isRemainOutput(output) ? IS_REMAIN_YES : IS_REMAIN_NO);
        finish.setSourceType(1);
        finish.setFinishStatus(FINISH_STATUS_PENDING);
        finish.setWarehouseUuid(context.order().getWarehouseUuid());
        finish.setOriginalRollNos(finishOriginalKey(context.roll()));
        finish.setPaperName(output.getPaperName());
        finish.setGramWeight(output.getGramWeight());
        finish.setFinishWidth(output.getFinishWidth());
        finish.setFinishDiameter(output.getFinishDiameter());
        finish.setFinishCoreDiameter(output.getFinishCoreDiameter());
        finish.setEstimateWeight(output.getEstimateWeight());
        finish.setEstimateWeightSnap(output.getEstimateWeight());
        finish.setRemark(isRemainOutput(output) ? "修边/余料" : "后续工艺最终产出：" + output.getOutputKey());
        return finish;
    }

    private boolean isRemainOutput(ProcessRoutePreviewVO.RouteOutputVO output) {
        return output.getIsRemain() != null && output.getIsRemain() == IS_REMAIN_YES;
    }

    private void saveRels(ProcessRouteContext context, FinishRoll finish, BigDecimal weight,
                          ProcessStageOutput output, SourceTrace trace) {
        Map<String, BigDecimal> contributions = trace.trace(output, context.roll().getUuid(), new HashSet<>());
        BigDecimal total = contributions.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            contributions = Map.of(context.roll().getUuid(), BigDecimal.ONE);
            total = BigDecimal.ONE;
        }
        BigDecimal allocated = BigDecimal.ZERO;
        int index = 0;
        for (Map.Entry<String, BigDecimal> entry : contributions.entrySet()) {
            BigDecimal ratio = index++ == contributions.size() - 1
                    ? new BigDecimal("100.00").subtract(allocated)
                    : entry.getValue().multiply(new BigDecimal("100.00"))
                            .divide(total, 2, RoundingMode.HALF_UP);
            FinishOriginalRel rel = new FinishOriginalRel();
            rel.setOrderUuid(context.order().getUuid());
            rel.setFinishUuid(finish.getUuid());
            rel.setOriginalUuid(entry.getKey());
            rel.setShareRatio(ratio.setScale(2, RoundingMode.HALF_UP));
            rel.setShareWeight(weight == null ? null : weight.multiply(rel.getShareRatio())
                    .divide(new BigDecimal("100.00"), 3, RoundingMode.HALF_UP));
            rel.setRemark("后续工艺最终产出");
            finishOriginalRelMapper.insert(rel);
            allocated = allocated.add(rel.getShareRatio());
        }
    }

    private SourceTrace sourceTrace(ProcessRouteContext context,
                                    Map<String, ProcessStageOutput> outputsByKey) {
        Map<String, ProcessStageOutput> outputs = new HashMap<>();
        outputsByKey.values().forEach(output -> {
            if (output != null && output.getUuid() != null) outputs.put(output.getUuid(), output);
        });
        if (stageInputRelMapper == null) return new SourceTrace(outputs, Map.of(), Map.of());
        List<ProcessStageInputRel> inputRelations = stageInputRelMapper.selectList(
                new LambdaQueryWrapper<ProcessStageInputRel>()
                        .eq(ProcessStageInputRel::getOrderUuid, context.order().getUuid()));
        Map<String, List<ProcessStageInputRel>> byStep = new HashMap<>();
        inputRelations.forEach(rel -> byStep.computeIfAbsent(rel.getStepUuid(), ignored -> new ArrayList<>()).add(rel));
        Map<String, List<FinishOriginalRel>> byFinish = new HashMap<>();
        List<FinishOriginalRel> finishRelations = finishOriginalRelMapper.selectList(
                new LambdaQueryWrapper<FinishOriginalRel>()
                        .eq(FinishOriginalRel::getOrderUuid, context.order().getUuid()));
        finishRelations.forEach(rel -> byFinish.computeIfAbsent(rel.getFinishUuid(), ignored -> new ArrayList<>()).add(rel));
        return new SourceTrace(outputs, byStep, byFinish);
    }

    private void markOutputFinishCreated(ProcessStageOutput output, FinishRoll finish) {
        output.setFinishRollUuid(finish.getUuid());
        output.setOutputStatus(OUTPUT_FINISH_CREATED);
        ConcurrencyGuard.requireRowUpdated(stageOutputMapper.updateById(output));
    }

    private void allocAndInsertFinish(FinishRoll finish) {
        for (int attempt = 0; attempt < 5; attempt++) {
            finish.setUuid(null);
            finish.setFinishRollNo(rollNoSequenceService.nextFinishRollNo());
            try {
                finishRollMapper.insert(finish);
                return;
            } catch (DuplicateKeyException ignored) {
                // 并发抢号后重试。
            }
        }
        throw new BusinessException("卷号分配冲突，请重试");
    }

    private int nextFinishRowSort(String orderUuid) {
        FinishRoll top = finishRollMapper.selectOne(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, orderUuid)
                .orderByDesc(FinishRoll::getRowSort)
                .last("LIMIT 1"));
        return top == null || top.getRowSort() == null ? 1 : top.getRowSort() + 1;
    }

    private String finishOriginalKey(OriginalRoll roll) {
        return StringUtils.hasText(roll.getRollNo()) ? roll.getRollNo() : roll.getUuid();
    }

    private record SourceTrace(Map<String, ProcessStageOutput> outputs,
                               Map<String, List<ProcessStageInputRel>> inputsByStep,
                               Map<String, List<FinishOriginalRel>> relationsByFinish) {
        Map<String, BigDecimal> trace(ProcessStageOutput output, String fallback, Set<String> visiting) {
            if (output == null || output.getUuid() == null || !visiting.add(output.getUuid())) {
                return Map.of(fallback, BigDecimal.ONE);
            }
            List<ProcessStageInputRel> inputs = output.getStepUuid() == null
                    ? List.of() : inputsByStep.getOrDefault(output.getStepUuid(), List.of());
            if (inputs.isEmpty()) {
                Map<String, BigDecimal> result = traceFinishSources(output, fallback);
                visiting.remove(output.getUuid());
                return result;
            }
            Map<String, BigDecimal> result = new HashMap<>();
            BigDecimal total = inputs.stream().map(rel -> outputs.get(rel.getInputOutputUuid()))
                    .map(SourceTrace::effectiveWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.signum() <= 0) total = BigDecimal.valueOf(inputs.size());
            for (ProcessStageInputRel input : inputs) {
                ProcessStageOutput child = outputs.get(input.getInputOutputUuid());
                BigDecimal childWeight = effectiveWeight(child);
                if (childWeight.signum() <= 0) childWeight = BigDecimal.ONE;
                BigDecimal factor = childWeight.divide(total, 12, RoundingMode.HALF_UP);
                trace(child, input.getOriginalUuid() == null ? fallback : input.getOriginalUuid(), visiting)
                        .forEach((source, value) -> result.merge(source, value.multiply(factor), BigDecimal::add));
            }
            visiting.remove(output.getUuid());
            return result;
        }

        private Map<String, BigDecimal> traceFinishSources(ProcessStageOutput output, String fallback) {
            List<FinishOriginalRel> relations = output.getFinishRollUuid() == null
                    ? List.of() : relationsByFinish.getOrDefault(output.getFinishRollUuid(), List.of());
            if (relations.isEmpty()) {
                return Map.of(output.getOriginalUuid() == null ? fallback : output.getOriginalUuid(),
                        effectiveWeight(output));
            }
            Map<String, BigDecimal> result = new HashMap<>();
            for (FinishOriginalRel relation : relations) {
                if (relation.getOriginalUuid() == null) continue;
                BigDecimal ratio = relation.getShareRatio() == null
                        ? BigDecimal.ONE.divide(BigDecimal.valueOf(relations.size()), 12, RoundingMode.HALF_UP)
                        : relation.getShareRatio().movePointLeft(2);
                result.merge(relation.getOriginalUuid(), ratio, BigDecimal::add);
            }
            return result.isEmpty() ? Map.of(fallback, BigDecimal.ONE) : result;
        }

        private static BigDecimal effectiveWeight(ProcessStageOutput output) {
            if (output == null) return BigDecimal.ZERO;
            if (output.getActualWeight() != null && output.getActualWeight().signum() > 0) {
                return output.getActualWeight();
            }
            return output.getEstimateWeight() == null ? BigDecimal.ZERO : output.getEstimateWeight();
        }
    }
}
