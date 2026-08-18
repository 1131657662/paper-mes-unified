package com.paper.mes.processorder.service;

import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.entity.ProcessStep;

/** Keeps rewind-only billing fields out of other process types. */
public final class ProcessStepWeightStatePolicy {

    private ProcessStepWeightStatePolicy() {
    }

    public static void clearWhenNotRewind(ProcessStep step) {
        if (step != null && !Integer.valueOf(FeeCalculator.STEP_TYPE_REWIND).equals(step.getStepType())) {
            step.setProcessWeight(null);
            step.setBillingWeightStatus(null);
            step.setBillingWeightBasis(null);
            step.setPricingDirty(0);
        }
    }
}
