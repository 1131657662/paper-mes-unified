package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.ProcessStep;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 混合工艺判定：不同工艺并存，或同一母卷存在多道工序。
 */
public final class ProcessMixProcessResolver {

    private ProcessMixProcessResolver() {
    }

    public static boolean isMix(List<ProcessStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return false;
        }
        Set<Integer> stepTypes = new HashSet<>();
        Set<String> rollUuids = new HashSet<>();
        for (ProcessStep step : steps) {
            if (step.getStepType() != null) {
                stepTypes.add(step.getStepType());
            }
            if (!rollUuids.add(step.getOriginalUuid())) {
                return true;
            }
        }
        return stepTypes.size() > 1;
    }
}
