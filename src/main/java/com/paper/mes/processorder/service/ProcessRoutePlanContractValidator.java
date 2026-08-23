package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;

/** Enforces the route plan needed to validate chained physical output geometry. */
final class ProcessRoutePlanContractValidator {

    private ProcessRoutePlanContractValidator() {
    }

    static void validate(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        if (stage.getStageLevel() == null || stage.getStageLevel() <= 1) {
            return;
        }
        if (stage.getPlan() == null) {
            throw new BusinessException("后续工艺必须提供完整加工排版方案");
        }
        if (stage.getPlan().getProcessMode() == null) {
            throw new BusinessException("后续工艺加工模式不能为空");
        }
        if (stage.getStepType() == FeeCalculator.STEP_TYPE_SAW
                && isEmpty(stage.getPlan().getFinishSpecs())) {
            throw new BusinessException("后续锯纸工艺必须提供成品排版规格");
        }
        if (stage.getStepType() == FeeCalculator.STEP_TYPE_REWIND
                && isEmpty(stage.getPlan().getSegments())) {
            throw new BusinessException("后续复卷工艺必须提供分段排版方案");
        }
    }

    private static boolean isEmpty(java.util.List<?> values) {
        return values == null || values.isEmpty();
    }
}
