package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ProcessStepRouteMutationGuard {

    private final ProcessStepMapper processStepMapper;

    public void requireOrdinaryMutationAllowed(ProcessStep step) {
        if (ownsChainTopology(step) || processStepMapper.hasActiveRouteReferences(step.getUuid())) {
            throw new BusinessException(ErrorCode.E001,
                    "链式工序不能通过普通工序接口修改或删除，请使用路线变更命令");
        }
    }

    private boolean ownsChainTopology(ProcessStep step) {
        return Integer.valueOf(2).equals(step.getInputType())
                || StringUtils.hasText(step.getInputOutputUuid())
                || StringUtils.hasText(step.getParentStepUuid())
                || (step.getStageLevel() != null && step.getStageLevel() > 1);
    }
}
