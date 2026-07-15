package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OnSitePlanPreviewer {

    public PlanPreviewVO preview(ProcessPlanDTO plan, String originalUuid) {
        PlanPreviewVO vo = shell(plan, originalUuid);
        vo.setFinishCount(0);
        vo.setTrimCount(0);
        vo.setTotalEstimateWeight(BigDecimal.ZERO);
        vo.setTotalTrimWeight(BigDecimal.ZERO);
        vo.setFinishes(List.of());
        vo.setReady(plan.getMainStepType() != null);
        if (!vo.isReady()) {
            vo.getErrors().add("现场定尺必须选择主工艺");
        }
        vo.setSummary("现场定尺不预生成成品号，实际成品和切边在回录时录入");
        return vo;
    }

    private PlanPreviewVO shell(ProcessPlanDTO plan, String originalUuid) {
        PlanPreviewVO vo = new PlanPreviewVO();
        vo.setOriginalUuid(originalUuid);
        vo.setProcessMode(plan.getProcessMode());
        vo.setMainStepType(plan.getMainStepType());
        vo.setRewindMode(plan.getRewindMode());
        vo.setSpareCount(plan.getSpareCount() == null ? 0 : plan.getSpareCount());
        return vo;
    }

}
