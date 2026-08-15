package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessParam;
import com.paper.mes.processorder.entity.ProcessStep;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Identifies duplicate merged-rewind main steps that must not bill again. */
public final class MergedRewindBillingScope {

    private static final int MAIN_STEP = 1;
    private static final int REWIND_STEP = 2;
    private static final int MERGED_REWIND_MODE = 5;

    private MergedRewindBillingScope() {
    }

    public static Set<String> suppressedMainStepUuids(Evidence evidence) {
        Set<String> owners = mergedOwners(evidence.steps(), evidence.params());
        if (owners.isEmpty()) return Set.of();

        Set<String> formalFinishIds = evidence.finishes().stream()
                .filter(MergedRewindBillingScope::isFormalFinish)
                .map(FinishRoll::getUuid)
                .collect(Collectors.toSet());
        Set<String> sourceOnly = new HashSet<>();
        Map<String, List<FinishOriginalRel>> groups = evidence.relations().stream()
                .filter(relation -> formalFinishIds.contains(relation.getFinishUuid()))
                .collect(Collectors.groupingBy(FinishOriginalRel::getFinishUuid));
        for (List<FinishOriginalRel> group : groups.values()) {
            Set<String> sourceIds = group.stream()
                    .map(FinishOriginalRel::getOriginalUuid)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            if (sourceIds.size() > 1 && sourceIds.stream().anyMatch(owners::contains)) {
                sourceOnly.addAll(sourceIds);
            }
        }
        sourceOnly.removeAll(owners);
        return evidence.steps().stream()
                .filter(step -> sourceOnly.contains(step.getOriginalUuid()))
                .filter(step -> Integer.valueOf(MAIN_STEP).equals(step.getIsMain()))
                .filter(step -> Integer.valueOf(REWIND_STEP).equals(step.getStepType()))
                .map(ProcessStep::getUuid)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> mergedOwners(List<ProcessStep> steps, List<ProcessParam> params) {
        Set<String> rewindMainKeys = steps.stream()
                .filter(step -> Integer.valueOf(MAIN_STEP).equals(step.getIsMain()))
                .filter(step -> Integer.valueOf(REWIND_STEP).equals(step.getStepType()))
                .map(step -> stepKey(step.getOriginalUuid(), step.getUuid()))
                .collect(Collectors.toSet());
        return params.stream()
                .filter(param -> Integer.valueOf(MERGED_REWIND_MODE).equals(param.getParamMode()))
                .filter(param -> rewindMainKeys.contains(stepKey(param.getOriginalUuid(), param.getStepUuid())))
                .map(ProcessParam::getOriginalUuid)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static boolean isFormalFinish(FinishRoll finish) {
        return finish != null
                && !Integer.valueOf(1).equals(finish.getIsSpare())
                && !Integer.valueOf(1).equals(finish.getIsRemain())
                && !Integer.valueOf(3).equals(finish.getRollNoStatus())
                && !FinishRollStatusPolicy.isScrapped(finish);
    }

    private static String stepKey(String originalUuid, String stepUuid) {
        return String.valueOf(originalUuid) + ':' + String.valueOf(stepUuid);
    }

    public record Evidence(List<ProcessStep> steps, List<ProcessParam> params,
                           List<FinishRoll> finishes, List<FinishOriginalRel> relations) {
    }
}
