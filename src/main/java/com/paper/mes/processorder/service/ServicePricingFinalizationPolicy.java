package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.entity.ProcessStep;

import java.util.List;

public final class ServicePricingFinalizationPolicy {

    private ServicePricingFinalizationPolicy() {
    }

    public static void requireFinalized(List<ProcessStep> steps) {
        long pending = steps.stream().filter(ServicePricingFinalizationPolicy::isPending).count();
        if (pending > 0) {
            throw new BusinessException("还有" + pending + "道附加工艺未定价，请核定单价、固定金额或选择免费后再结算");
        }
    }

    public static boolean isPending(ProcessStep step) {
        if (!isService(step.getStepType())) return false;
        int mode = step.getBillingMode() == null ? ProcessStepPricingPolicy.STANDARD : step.getBillingMode();
        if (mode == ProcessStepPricingPolicy.FIXED_AMOUNT || mode == ProcessStepPricingPolicy.FREE) return false;
        boolean standardMissing = step.getUnitPrice() == null || step.getUnitPrice().signum() <= 0;
        boolean billingMissing = step.getBillingUnitPrice() == null || step.getBillingUnitPrice().signum() <= 0;
        return standardMissing && billingMissing;
    }

    private static boolean isService(Integer stepType) {
        return Integer.valueOf(FeeCalculator.STEP_TYPE_STRIP_SORT).equals(stepType)
                || Integer.valueOf(FeeCalculator.STEP_TYPE_REPACKAGE).equals(stepType);
    }
}
