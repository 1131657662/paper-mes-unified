package com.paper.mes.processorder.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessParam;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.service.FinishRollStatusPolicy;
import com.paper.mes.processorder.service.ProcessModePolicy;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 加工单首次打印前的工艺与正式成品完整性校验。 */
final class ProcessOrderPrintableConfigValidator {

    private static final int PROCESS_MODE_ON_SITE = 2;
    private static final int PROCESS_MODE_DIRECT_SHIP = 3;
    private static final int MAIN_STEP = 1;
    private static final int REWIND_STEP = 2;
    private static final int SPARE_NO = 0;
    private static final int REMAIN_YES = 1;
    private static final int ROLL_NO_VOID = 3;

    private ProcessOrderPrintableConfigValidator() {
    }

    static void validate(List<OriginalRoll> rolls, List<FinishRoll> finishes,
                         ProcessEvidence evidence) {
        Set<String> formalFinishIds = finishes.stream()
                .filter(ProcessOrderPrintableConfigValidator::isFormalFinish)
                .map(FinishRoll::getUuid)
                .collect(Collectors.toSet());
        SourceCoverage coverage = new SourceCoverage(
                formalSourceIds(formalFinishIds, evidence.relations()),
                mergedRewindSourceIds(formalFinishIds, evidence));
        PrintEvidence printEvidence = new PrintEvidence(finishes, evidence.steps(), coverage);
        for (OriginalRoll roll : rolls) {
            validateRoll(roll, printEvidence);
        }
    }

    private static void validateRoll(OriginalRoll roll, PrintEvidence evidence) {
        if (roll.getDispositionAction() != null) return;
        if (roll.getProcessMode() != null && roll.getProcessMode() == PROCESS_MODE_DIRECT_SHIP) return;
        boolean missingMainStep = ProcessModePolicy.requiresMainProcess(roll.getProcessMode())
                && !hasMainStep(roll, evidence.steps());
        boolean mergedRewindSource = evidence.coverage().mergedRewindIds().contains(roll.getUuid())
                && Integer.valueOf(REWIND_STEP).equals(roll.getMainStepType());
        if (missingMainStep && !mergedRewindSource) {
            throw new BusinessException("原纸缺少主工序，不能打印：" + rollKey(roll));
        }
        if (roll.getProcessMode() != null && roll.getProcessMode() == PROCESS_MODE_ON_SITE) return;
        if (!evidence.coverage().formalSourceIds().contains(roll.getUuid())
                && !hasLegacyFinish(roll, evidence.finishes())) {
            throw new BusinessException("原纸尚未配置正式成品号，不能打印：" + rollKey(roll));
        }
    }

    private static boolean hasMainStep(OriginalRoll roll, List<ProcessStep> steps) {
        return steps.stream().anyMatch(step -> roll.getUuid().equals(step.getOriginalUuid())
                && step.getIsMain() != null && step.getIsMain() == MAIN_STEP);
    }

    private static Set<String> formalSourceIds(Set<String> formalFinishIds,
                                                List<FinishOriginalRel> relations) {
        return relations.stream()
                .filter(relation -> formalFinishIds.contains(relation.getFinishUuid()))
                .map(FinishOriginalRel::getOriginalUuid)
                .collect(Collectors.toSet());
    }

    private static Set<String> mergedRewindSourceIds(Set<String> formalFinishIds,
                                                     ProcessEvidence evidence) {
        Set<String> rewindMainKeys = rewindMainStepKeys(evidence.steps());
        Set<String> mergedOwners = evidence.params().stream()
                .filter(param -> Integer.valueOf(5).equals(param.getParamMode()))
                .filter(param -> rewindMainKeys.contains(stepKey(param.getOriginalUuid(), param.getStepUuid())))
                .map(ProcessParam::getOriginalUuid)
                .collect(Collectors.toSet());
        return evidence.relations().stream()
                .filter(relation -> formalFinishIds.contains(relation.getFinishUuid()))
                .collect(Collectors.groupingBy(FinishOriginalRel::getFinishUuid))
                .values().stream()
                .filter(ProcessOrderPrintableConfigValidator::hasMultipleSources)
                .filter(group -> group.stream().anyMatch(relation -> mergedOwners.contains(relation.getOriginalUuid())))
                .flatMap(List::stream)
                .map(FinishOriginalRel::getOriginalUuid)
                .collect(Collectors.toSet());
    }

    private static boolean hasMultipleSources(List<FinishOriginalRel> relations) {
        return relations.stream()
                .map(FinishOriginalRel::getOriginalUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(2)
                .count() == 2;
    }

    private static Set<String> rewindMainStepKeys(List<ProcessStep> steps) {
        return steps.stream()
                .filter(step -> Integer.valueOf(MAIN_STEP).equals(step.getIsMain()))
                .filter(step -> Integer.valueOf(REWIND_STEP).equals(step.getStepType()))
                .filter(step -> StringUtils.hasText(step.getOriginalUuid()) && StringUtils.hasText(step.getUuid()))
                .map(step -> stepKey(step.getOriginalUuid(), step.getUuid()))
                .collect(Collectors.toSet());
    }

    private static String stepKey(String originalUuid, String stepUuid) {
        if (!StringUtils.hasText(originalUuid) || !StringUtils.hasText(stepUuid)) return "";
        return originalUuid + ':' + stepUuid;
    }

    private static boolean hasLegacyFinish(OriginalRoll roll, List<FinishRoll> finishes) {
        return finishes.stream().anyMatch(finish -> rollKey(roll).equals(finish.getOriginalRollNos())
                && isFormalFinish(finish));
    }

    private static boolean isFormalFinish(FinishRoll finish) {
        boolean formal = finish.getIsSpare() == null || finish.getIsSpare() == SPARE_NO;
        boolean finalProduct = finish.getIsRemain() == null || finish.getIsRemain() != REMAIN_YES;
        boolean active = finish.getRollNoStatus() == null || finish.getRollNoStatus() != ROLL_NO_VOID;
        return formal && finalProduct && active && !FinishRollStatusPolicy.isScrapped(finish);
    }

    private static String rollKey(OriginalRoll roll) {
        return StringUtils.hasText(roll.getRollNo()) ? roll.getRollNo() : roll.getUuid();
    }

    record ProcessEvidence(List<ProcessStep> steps, List<ProcessParam> params,
                           List<FinishOriginalRel> relations) {
    }

    private record SourceCoverage(Set<String> formalSourceIds, Set<String> mergedRewindIds) {
    }

    private record PrintEvidence(List<FinishRoll> finishes, List<ProcessStep> steps,
                                 SourceCoverage coverage) {
    }
}
