package com.paper.mes.processorder.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
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
    private static final int SPARE_NO = 0;
    private static final int REMAIN_YES = 1;
    private static final int ROLL_NO_VOID = 3;

    private ProcessOrderPrintableConfigValidator() {
    }

    static void validate(List<OriginalRoll> rolls, List<FinishRoll> finishes,
                         List<ProcessStep> steps, List<FinishOriginalRel> relations) {
        Set<String> formalFinishIds = finishes.stream()
                .filter(ProcessOrderPrintableConfigValidator::isFormalFinish)
                .map(FinishRoll::getUuid)
                .collect(Collectors.toSet());
        Set<String> relatedOriginalIds = relations.stream()
                .filter(relation -> formalFinishIds.contains(relation.getFinishUuid()))
                .map(FinishOriginalRel::getOriginalUuid)
                .collect(Collectors.toSet());
        for (OriginalRoll roll : rolls) validateRoll(roll, finishes, steps, relatedOriginalIds);
    }

    private static void validateRoll(OriginalRoll roll, List<FinishRoll> finishes,
                                     List<ProcessStep> steps, Set<String> relatedOriginalIds) {
        if (roll.getProcessMode() != null && roll.getProcessMode() == PROCESS_MODE_DIRECT_SHIP) return;
        if (ProcessModePolicy.requiresMainProcess(roll.getProcessMode()) && !hasMainStep(roll, steps)) {
            throw new BusinessException("原纸缺少主工序，不能打印：" + rollKey(roll));
        }
        if (roll.getProcessMode() != null && roll.getProcessMode() == PROCESS_MODE_ON_SITE) return;
        if (!relatedOriginalIds.contains(roll.getUuid()) && !hasLegacyFinish(roll, finishes)) {
            throw new BusinessException("原纸尚未配置正式成品号，不能打印：" + rollKey(roll));
        }
    }

    private static boolean hasMainStep(OriginalRoll roll, List<ProcessStep> steps) {
        return steps.stream().anyMatch(step -> roll.getUuid().equals(step.getOriginalUuid())
                && step.getIsMain() != null && step.getIsMain() == MAIN_STEP);
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
}
