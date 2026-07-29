package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessParam;
import com.paper.mes.processorder.entity.ProcessStageInputRel;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessParamMapper;
import com.paper.mes.processorder.mapper.ProcessStageInputRelMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessRouteCleanupService {

    private final ProcessRouteFinishCleanup finishCleanup;
    private final ProcessStageInputRelMapper stageInputRelMapper;
    private final ProcessStageOutputMapper stageOutputMapper;
    private final ProcessParamMapper processParamMapper;
    private final ProcessStepMapper processStepMapper;

    public void clearExistingRoute(ProcessRouteContext context) {
        OriginalRoll roll = context.roll();
        finishCleanup.clear(context);
        stageInputRelMapper.delete(new LambdaQueryWrapper<ProcessStageInputRel>()
                .eq(ProcessStageInputRel::getOriginalUuid, roll.getUuid()));
        stageOutputMapper.delete(new LambdaQueryWrapper<ProcessStageOutput>()
                .eq(ProcessStageOutput::getOriginalUuid, roll.getUuid()));
        processParamMapper.delete(new LambdaQueryWrapper<ProcessParam>()
                .eq(ProcessParam::getOriginalUuid, roll.getUuid()));
        processStepMapper.delete(new LambdaQueryWrapper<ProcessStep>()
                .eq(ProcessStep::getOriginalUuid, roll.getUuid()));
    }

}
