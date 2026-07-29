package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProcessRouteExistingOutputLoader {

    private final ProcessStageOutputMapper stageOutputMapper;
    private final FinishRollMapper finishRollMapper;
    private final FinishOriginalRelMapper finishOriginalRelMapper;
    private final ProcessStepMapper processStepMapper;

    List<ProcessStageOutput> stageOutputs(ProcessRouteContext context) {
        return stageOutputMapper.selectList(new LambdaQueryWrapper<ProcessStageOutput>()
                .eq(ProcessStageOutput::getOrderUuid, context.order().getUuid())
                .eq(ProcessStageOutput::getOriginalUuid, context.roll().getUuid()));
    }

    List<FinishRoll> finishRolls(ProcessRouteContext context) {
        return finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, context.order().getUuid()));
    }

    Set<String> relatedFinishUuids(ProcessRouteContext context, Collection<FinishRoll> finishes) {
        List<String> finishUuids = finishes.stream().map(FinishRoll::getUuid).distinct().toList();
        if (finishUuids.isEmpty()) {
            return Set.of();
        }
        return finishOriginalRelMapper.selectList(new LambdaQueryWrapper<FinishOriginalRel>()
                        .eq(FinishOriginalRel::getOrderUuid, context.order().getUuid())
                        .eq(FinishOriginalRel::getOriginalUuid, context.roll().getUuid())
                        .in(FinishOriginalRel::getFinishUuid, finishUuids))
                .stream()
                .map(FinishOriginalRel::getFinishUuid)
                .collect(Collectors.toSet());
    }

    ProcessStep latestStep(ProcessRouteContext context) {
        return processStepMapper.selectOne(new LambdaQueryWrapper<ProcessStep>()
                .eq(ProcessStep::getOrderUuid, context.order().getUuid())
                .eq(ProcessStep::getOriginalUuid, context.roll().getUuid())
                .orderByDesc(ProcessStep::getStepSort)
                .last("LIMIT 1"));
    }
}
