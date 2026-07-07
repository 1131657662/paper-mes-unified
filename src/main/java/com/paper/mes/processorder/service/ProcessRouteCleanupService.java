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
        List<FinishRoll> existing = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, context.order().getUuid())
                .eq(FinishRoll::getOriginalRollNos, finishOriginalKey(roll))
                .ne(FinishRoll::getRollNoStatus, ROLL_NO_VOID));
        for (FinishRoll finish : existing) {
            if (finish.getActualWeight() != null) {
                throw new BusinessException(ErrorCode.E003, "已有回录重量的成品不可重新配置后续工艺");
            }
        }
        if (!existing.isEmpty()) {
            finishOriginalRelMapper.delete(new LambdaQueryWrapper<FinishOriginalRel>()
                    .in(FinishOriginalRel::getFinishUuid, existing.stream().map(FinishRoll::getUuid).toList()));
        }
        voidFinishRolls(existing);
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
