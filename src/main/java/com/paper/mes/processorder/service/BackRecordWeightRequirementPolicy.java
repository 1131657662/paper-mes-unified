package com.paper.mes.processorder.service;

import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Determines which source rolls require measured weight before back-record completion. */
public final class BackRecordWeightRequirementPolicy {

    private BackRecordWeightRequirementPolicy() {
    }

    public static Set<String> requiredRollUuids(List<OriginalRoll> rolls, List<ProcessStep> steps,
                                                List<FinishOriginalRel> relations) {
        Map<String, OriginalRoll> rollByUuid = indexRolls(rolls);
        Set<String> required = new LinkedHashSet<>();
        for (ProcessStep step : steps == null ? List.<ProcessStep>of() : steps) {
            if (!isStandardTonnageRewind(step)) continue;
            required.addAll(sourceRollUuids(step, rollByUuid, relations));
        }
        return required;
    }

    public static boolean isStandardTonnageRewind(ProcessStep step) {
        if (step == null || !Integer.valueOf(FeeCalculator.STEP_TYPE_REWIND).equals(step.getStepType())) {
            return false;
        }
        int mode = step.getBillingMode() == null ? ProcessStepPricingPolicy.STANDARD : step.getBillingMode();
        return mode == ProcessStepPricingPolicy.STANDARD;
    }

    private static Set<String> sourceRollUuids(ProcessStep step, Map<String, OriginalRoll> rolls,
                                                List<FinishOriginalRel> relations) {
        Set<String> result = new LinkedHashSet<>();
        if (rolls.containsKey(step.getOriginalUuid())) result.add(step.getOriginalUuid());
        if (relations == null || relations.isEmpty()) return result;
        Set<String> finishUuids = relatedFinishUuids(step.getOriginalUuid(), relations);
        for (FinishOriginalRel relation : relations) {
            if (finishUuids.contains(relation.getFinishUuid()) && rolls.containsKey(relation.getOriginalUuid())) {
                result.add(relation.getOriginalUuid());
            }
        }
        return result;
    }

    private static Set<String> relatedFinishUuids(String originalUuid, List<FinishOriginalRel> relations) {
        Set<String> result = new LinkedHashSet<>();
        for (FinishOriginalRel relation : relations) {
            if (Objects.equals(originalUuid, relation.getOriginalUuid())) result.add(relation.getFinishUuid());
        }
        return result;
    }

    private static Map<String, OriginalRoll> indexRolls(List<OriginalRoll> rolls) {
        Map<String, OriginalRoll> result = new LinkedHashMap<>();
        for (OriginalRoll roll : rolls == null ? List.<OriginalRoll>of() : rolls) {
            result.put(roll.getUuid(), roll);
        }
        return result;
    }
}
