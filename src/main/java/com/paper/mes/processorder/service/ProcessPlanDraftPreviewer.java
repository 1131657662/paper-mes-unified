package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
class ProcessPlanDraftPreviewer {

    private final ProcessOrderService orderService;
    private final ProcessPlanMapper planMapper;
    private final SawPlanPreviewer sawPlanPreviewer;
    private final OnSitePlanPreviewer onSitePlanPreviewer;
    private final ServiceOnlyProcessPolicy serviceOnlyProcessPolicy;

    ProcessPlanDraftPreviewContext createContext(ProcessOrder order,
                                                 Map<String, OriginalRoll> sourceRolls,
                                                 List<ProcessPlanSaveCandidate> candidates) {
        List<String> serviceRollUuids = candidates.stream()
                .filter(candidate -> ProcessModePolicy.isServiceOnly(candidate.plan().getProcessMode()))
                .map(candidate -> candidate.roll().getUuid())
                .distinct()
                .toList();
        Set<String> configured = serviceOnlyProcessPolicy.configuredRollUuids(serviceRollUuids);
        return new ProcessPlanDraftPreviewContext(order, sourceRolls, configured);
    }

    PlanPreviewVO preview(ProcessPlanDraftPreviewContext context, OriginalRoll roll, ProcessPlanDTO plan) {
        ProcessModePolicy.requireValid(plan.getProcessMode(), plan.getMainStepType());
        if (ProcessModePolicy.isDirectShip(plan.getProcessMode())) {
            return planMapper.directPreview(plan, roll.getUuid());
        }
        if (ProcessModePolicy.isServiceOnly(plan.getProcessMode())) {
            return serviceOnlyPreview(context, roll, plan);
        }
        if (Integer.valueOf(ProcessModePolicy.ON_SITE).equals(plan.getProcessMode())) {
            return onSitePlanPreviewer.preview(plan, roll.getUuid());
        }
        if (Integer.valueOf(FeeCalculator.STEP_TYPE_SAW).equals(plan.getMainStepType())) {
            return sawPlanPreviewer.preview(plan, roll);
        }
        return rewindPreview(context, roll, plan);
    }

    private PlanPreviewVO serviceOnlyPreview(ProcessPlanDraftPreviewContext context,
                                             OriginalRoll roll, ProcessPlanDTO plan) {
        int finishCount = roll.getPieceNum() == null ? 1 : roll.getPieceNum();
        return planMapper.serviceOnlyPreview(plan, roll.getUuid(), finishCount,
                context.configuredServiceRollUuids().contains(roll.getUuid()));
    }

    private PlanPreviewVO rewindPreview(ProcessPlanDraftPreviewContext context,
                                        OriginalRoll roll, ProcessPlanDTO plan) {
        try {
            FinishPreviewVO preview = orderService.previewRewindPlan(
                    new RewindPlanPreviewContext(context.order(), roll, context.sourceRolls()),
                    planMapper.toPreviewDto(plan));
            return planMapper.toPlanPreview(plan, roll.getUuid(), preview);
        } catch (BusinessException e) {
            return errorPreview(plan, roll.getUuid(), e.getMessage());
        }
    }

    private PlanPreviewVO errorPreview(ProcessPlanDTO plan, String rollUuid, String message) {
        PlanPreviewVO preview = new PlanPreviewVO();
        preview.setOriginalUuid(rollUuid);
        preview.setProcessMode(plan.getProcessMode());
        preview.setMainStepType(plan.getMainStepType());
        preview.setRewindMode(plan.getRewindMode());
        preview.setSpareCount(plan.getSpareCount() == null ? 0 : plan.getSpareCount());
        preview.setReady(false);
        preview.getErrors().add(message);
        preview.setSummary("方案存在错误，请按提示修正");
        return preview;
    }
}
