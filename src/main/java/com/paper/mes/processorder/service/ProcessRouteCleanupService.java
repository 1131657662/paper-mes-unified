package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessParam;
import com.paper.mes.processorder.entity.ProcessStageInputRel;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessParamMapper;
import com.paper.mes.processorder.mapper.ProcessStageInputRelMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProcessRouteCleanupService {

    private static final int ROLL_NO_VOID = 3;

    private final FinishRollMapper finishRollMapper;
    private final FinishOriginalRelMapper finishOriginalRelMapper;
    private final ProcessStageInputRelMapper stageInputRelMapper;
    private final ProcessStageOutputMapper stageOutputMapper;
    private final ProcessParamMapper processParamMapper;
    private final ProcessStepMapper processStepMapper;

    public void clearExistingRoute(ProcessRouteContext context) {
        OriginalRoll roll = context.roll();
        voidExistingFinishes(context, roll);
        stageInputRelMapper.delete(new LambdaQueryWrapper<ProcessStageInputRel>()
                .eq(ProcessStageInputRel::getOriginalUuid, roll.getUuid()));
        stageOutputMapper.delete(new LambdaQueryWrapper<ProcessStageOutput>()
                .eq(ProcessStageOutput::getOriginalUuid, roll.getUuid()));
        processParamMapper.delete(new LambdaQueryWrapper<ProcessParam>()
                .eq(ProcessParam::getOriginalUuid, roll.getUuid()));
        processStepMapper.delete(new LambdaQueryWrapper<ProcessStep>()
                .eq(ProcessStep::getOriginalUuid, roll.getUuid()));
    }

    private void voidExistingFinishes(ProcessRouteContext context, OriginalRoll roll) {
        List<FinishOriginalRel> rollRelations = safeRelations(finishOriginalRelMapper.selectList(
                new LambdaQueryWrapper<FinishOriginalRel>()
                        .eq(FinishOriginalRel::getOriginalUuid, roll.getUuid())));
        Set<String> relatedFinishUuids = new LinkedHashSet<>(rollRelations.stream()
                .map(FinishOriginalRel::getFinishUuid).toList());
        List<FinishRoll> existing = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, context.order().getUuid())
                .and(wrapper -> {
                    wrapper.eq(FinishRoll::getOriginalRollNos, finishOriginalKey(roll));
                    if (!relatedFinishUuids.isEmpty()) {
                        wrapper.or().in(FinishRoll::getUuid, relatedFinishUuids);
                    }
                }));
        List<FinishOriginalRel> allRelatedRelations = relatedFinishUuids.isEmpty()
                ? List.of()
                : safeRelations(finishOriginalRelMapper.selectList(new LambdaQueryWrapper<FinishOriginalRel>()
                .in(FinishOriginalRel::getFinishUuid, relatedFinishUuids)));
        rejectActiveSharedFinishes(roll, relatedFinishUuids, existing, allRelatedRelations);
        Set<String> singleSourceFinishUuids = allRelatedRelations.stream()
                .collect(Collectors.groupingBy(FinishOriginalRel::getFinishUuid, Collectors.mapping(
                        FinishOriginalRel::getOriginalUuid, Collectors.toSet())))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() == 1 && entry.getValue().contains(roll.getUuid()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String ownerKey = finishOriginalKey(roll);
        List<FinishRoll> activeOwned = existing.stream()
                .filter(finish -> !Integer.valueOf(ROLL_NO_VOID).equals(finish.getRollNoStatus()))
                .filter(finish -> Objects.equals(ownerKey, finish.getOriginalRollNos())
                        || singleSourceFinishUuids.contains(finish.getUuid()))
                .toList();
        for (FinishRoll finish : activeOwned) {
            if (finish.getActualWeight() != null) {
                throw new BusinessException(ErrorCode.E003, "已有回录重量的成品不可重新配置后续工艺");
            }
        }
        LambdaQueryWrapper<FinishOriginalRel> relationDelete = new LambdaQueryWrapper<FinishOriginalRel>()
                .eq(FinishOriginalRel::getOriginalUuid, roll.getUuid());
        finishOriginalRelMapper.delete(relationDelete);
        voidFinishRolls(activeOwned);
    }

    private void rejectActiveSharedFinishes(OriginalRoll roll, Set<String> relatedFinishUuids,
                                            List<FinishRoll> finishes, List<FinishOriginalRel> relations) {
        Map<String, Set<String>> sourceUuidsByFinish = relations.stream()
                .collect(Collectors.groupingBy(FinishOriginalRel::getFinishUuid, Collectors.mapping(
                        FinishOriginalRel::getOriginalUuid, Collectors.toSet())));
        boolean sourceOfActiveSharedFinish = finishes.stream()
                .anyMatch(finish -> relatedFinishUuids.contains(finish.getUuid())
                        && sourceUuidsByFinish.getOrDefault(finish.getUuid(), Set.of()).size() > 1
                        && !Integer.valueOf(ROLL_NO_VOID).equals(finish.getRollNoStatus()));
        if (sourceOfActiveSharedFinish) {
            throw new BusinessException(ErrorCode.E003,
                    "该母卷正被其他母卷的合并复卷配置使用，请先从配置拥有母卷重新配置成品");
        }
    }

    private List<FinishOriginalRel> safeRelations(List<FinishOriginalRel> relations) {
        return relations == null ? List.of() : relations;
    }

    private void voidFinishRolls(List<FinishRoll> finishes) {
        if (finishes.isEmpty()) {
            return;
        }
        List<String> finishUuids = finishes.stream().map(FinishRoll::getUuid).toList();
        int updated = finishRollMapper.update(null, new LambdaUpdateWrapper<FinishRoll>()
                .in(FinishRoll::getUuid, finishUuids)
                .ne(FinishRoll::getRollNoStatus, ROLL_NO_VOID)
                .isNull(FinishRoll::getActualWeight)
                .set(FinishRoll::getRollNoStatus, ROLL_NO_VOID)
                .set(FinishRoll::getUpdateTime, LocalDateTime.now())
                .set(FinishRoll::getUpdateBy, AuthContextHolder.currentDisplayName())
                .setSql("version = version + 1"));
        if (updated != finishUuids.size()) {
            throw new BusinessException(ErrorCode.E006, "旧成品卷号状态已变化，请刷新后重试");
        }
    }

    private String finishOriginalKey(OriginalRoll roll) {
        return StringUtils.hasText(roll.getRollNo()) ? roll.getRollNo() : roll.getUuid();
    }
}
