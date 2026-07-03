package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProcessRouteSourceConsumer {

    private static final int OUTPUT_INTERMEDIATE = 1;
    private static final int OUTPUT_CONSUMED = 2;
    private static final int ROLL_NO_VOID = 3;

    private final ProcessStageOutputMapper stageOutputMapper;
    private final FinishRollMapper finishRollMapper;
    private final FinishOriginalRelMapper finishOriginalRelMapper;

    public void consume(Collection<ProcessStageOutput> outputs) {
        Set<String> consumed = new HashSet<>();
        for (ProcessStageOutput output : outputs) {
            if (output.getUuid() == null || !consumed.add(output.getUuid())) {
                continue;
            }
            voidLinkedFinish(output);
            output.setOutputType(OUTPUT_INTERMEDIATE);
            output.setOutputStatus(OUTPUT_CONSUMED);
            stageOutputMapper.updateById(output);
        }
    }

    private void voidLinkedFinish(ProcessStageOutput output) {
        if (!StringUtils.hasText(output.getFinishRollUuid())) {
            return;
        }
        FinishRoll finish = finishRollMapper.selectById(output.getFinishRollUuid());
        if (finish == null) {
            return;
        }
        if (finish.getActualWeight() != null) {
            throw new BusinessException("已有回录实重的成品不能再进入下道工艺：" + finish.getFinishRollNo());
        }
        finishOriginalRelMapper.delete(new LambdaQueryWrapper<FinishOriginalRel>()
                .eq(FinishOriginalRel::getFinishUuid, finish.getUuid()));
        finish.setRollNoStatus(ROLL_NO_VOID);
        finishRollMapper.updateById(finish);
    }
}
