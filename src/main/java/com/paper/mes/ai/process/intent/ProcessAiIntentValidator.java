package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class ProcessAiIntentValidator {

    public void validate(ProcessAiExtractionResult result, ProcessAiOrderContext context) {
        Set<String> allowed = new HashSet<>();
        context.rolls().forEach(roll -> allowed.add(roll.shortRef()));
        Set<String> assigned = new HashSet<>();
        for (ProcessAiAssignment assignment : result.assignments()) {
            validateAssignment(assignment, allowed, assigned, result.needsClarification());
        }
        requireClarificationConsistency(result);
    }

    private void validateAssignment(ProcessAiAssignment value, Set<String> allowed,
                                    Set<String> assigned, boolean clarification) {
        LinkedHashSet<String> sources = new LinkedHashSet<>(value.sourceRollRefs());
        if (sources.size() != value.sourceRollRefs().size() || !allowed.containsAll(sources)) {
            throw invalid("AI_INTENT_SOURCE_INVALID", "AI解析引用了无效或重复的母卷");
        }
        if (!sources.contains(value.ownerRollRef())) {
            throw invalid("AI_INTENT_OWNER_INVALID", "AI解析的owner不在来源母卷中");
        }
        Set<String> expectedCovered = new LinkedHashSet<>(sources);
        expectedCovered.remove(value.ownerRollRef());
        if (!expectedCovered.equals(new LinkedHashSet<>(value.coveredRollRefs()))) {
            throw invalid("AI_INTENT_COVERAGE_INVALID", "AI解析的covered母卷不闭合");
        }
        for (String source : sources) {
            if (!assigned.add(source) && !clarification) {
                throw invalid("AI_INTENT_SOURCE_OVERLAP", "同一母卷被多套AI方案覆盖");
            }
        }
        requireMatchingIntent(value);
        validateWeightSplit(value.rewindIntent());
        validateAncillary(value.ancillaryRequirements());
    }

    private void requireMatchingIntent(ProcessAiAssignment value) {
        boolean rewind = "REWIND".equals(value.processType());
        boolean invalidRewind = rewind && (value.rewindIntent() == null || value.sawIntent() != null);
        boolean invalidSaw = !rewind && (value.sawIntent() == null || value.rewindIntent() != null);
        if (invalidRewind || invalidSaw) {
            throw invalid("AI_INTENT_PROCESS_MISMATCH", "AI解析的加工类型与工艺意图不一致");
        }
    }

    private void validateWeightSplit(ProcessAiRewindIntent rewind) {
        if (rewind == null || rewind.diameterRule() == null
                || !"WEIGHT_SPLIT".equals(rewind.diameterRule().type())) return;
        ProcessAiDiameterRule rule = rewind.diameterRule();
        if (rule.parts() == null || rule.ratios() == null || rule.parts() != rule.ratios().size()) {
            throw invalid("AI_INTENT_WEIGHT_SPLIT_INVALID", "重量分卷件数与比例数量不一致");
        }
        BigDecimal total = rule.ratios().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("100")) != 0) {
            throw invalid("AI_INTENT_WEIGHT_SPLIT_INVALID", "重量分卷比例合计必须为100");
        }
    }

    private void validateAncillary(ProcessAiAncillaryRequirements ancillary) {
        if (ancillary == null) return;
        if (ancillary.label() != null && ancillary.label().createsServiceStep()) {
            throw invalid("AI_INTENT_LABEL_STEP_FORBIDDEN", "标签不能创建附加工序");
        }
        if (ancillary.packaging() != null && !ancillary.packaging().createsServiceStep()) {
            throw invalid("AI_INTENT_PACKAGING_STEP_REQUIRED", "包装候选必须标记为附加工序");
        }
    }

    private void requireClarificationConsistency(ProcessAiExtractionResult result) {
        boolean blocked = !result.conflicts().isEmpty() || !result.clarificationQuestions().isEmpty();
        if (blocked && !result.needsClarification()) {
            throw invalid("AI_INTENT_CLARIFICATION_REQUIRED", "AI解析存在冲突但未进入澄清状态");
        }
    }

    private BusinessException invalid(String code, String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, code, message);
    }
}
