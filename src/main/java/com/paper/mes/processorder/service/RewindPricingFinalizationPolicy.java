package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.entity.ProcessStep;

import java.util.List;

/** Final billing gate for standard tonnage rewind steps. */
public final class RewindPricingFinalizationPolicy {

    private RewindPricingFinalizationPolicy() {
    }

    public static void requireFinalized(List<ProcessStep> steps) {
        requireFinalized(steps, false);
    }

    public static void requireFinalizedForSettlement(List<ProcessStep> steps) {
        requireFinalized(steps, true);
    }

    private static void requireFinalized(List<ProcessStep> steps, boolean allowLegacyUnclassified) {
        long pending = steps.stream()
                .filter(step -> !allowLegacyUnclassified || step.getBillingWeightStatus() != null)
                .filter(RewindPricingFinalizationPolicy::isPending)
                .count();
        if (pending > 0) {
            throw new BusinessException("还有" + pending
                    + "道标准复卷工序未完成实测计费，请逐卷称重并重新核定后再完成或结算");
        }
    }

    public static boolean isPending(ProcessStep step) {
        if (!Integer.valueOf(FeeCalculator.STEP_TYPE_REWIND).equals(step.getStepType())) return false;
        if ("FIXED".equalsIgnoreCase(step.getBillingWeightBasis())) return false;
        int mode = step.getBillingMode() == null ? ProcessStepPricingPolicy.STANDARD : step.getBillingMode();
        if (mode != ProcessStepPricingPolicy.STANDARD) return false;
        return !"MEASURED".equalsIgnoreCase(step.getBillingWeightStatus())
                || !Integer.valueOf(0).equals(step.getPricingDirty());
    }
}
