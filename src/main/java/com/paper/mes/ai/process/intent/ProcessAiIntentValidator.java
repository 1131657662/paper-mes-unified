package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

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
        validateQuantity(value.rewindIntent(), sources, allowed);
        validateAncillary(value.ancillaryRequirements());
        validateCustomerSpecs(value.customerSpecs());
    }

    private void validateQuantity(ProcessAiRewindIntent rewind, LinkedHashSet<String> sources,
                                  Set<String> allowed) {
        if (rewind == null || rewind.quantityIntent() == null) return;
        ProcessAiQuantityIntent quantity = rewind.quantityIntent();
        if (!"REPEAT_WIDTH".equals(quantity.type())) {
            throw invalid("AI_QUANTITY_TYPE_INVALID", "重复复卷数量类型无效");
        }
        if ("PER_SOURCE".equals(quantity.scope())) {
            if (!quantity.sourceAllocation().isEmpty()) {
                throw invalid("AI_QUANTITY_ALLOCATION_INVALID", "每条母卷模式不应携带分配表");
            }
            return;
        }
        if (!"TOTAL".equals(quantity.scope()) || quantity.sourceAllocation().isEmpty()) {
            throw invalid("AI_QUANTITY_ALLOCATION_REQUIRED", "全单数量必须提供母卷分配");
        }
        Map<String, Integer> allocation = quantity.sourceAllocation().stream().collect(
                Collectors.toMap(ProcessAiSourceAllocation::sourceRollRef,
                        ProcessAiSourceAllocation::count, Integer::sum));
        if (allocation.size() != quantity.sourceAllocation().size()
                || !allowed.containsAll(allocation.keySet())
                || !sources.containsAll(allocation.keySet())) {
            throw invalid("AI_QUANTITY_SOURCE_INVALID", "数量分配引用了无效或未选中的母卷");
        }
        int total = allocation.values().stream().mapToInt(Integer::intValue).sum();
        if (total != quantity.count()) {
            throw invalid("AI_QUANTITY_ALLOCATION_NOT_CLOSED", "数量分配合计必须等于全单数量");
        }
    }

    private void requireMatchingIntent(ProcessAiAssignment value) {
        if (isServiceOnly(value)) {
            if (value.rewindIntent() != null || value.sawIntent() != null
                    || value.ancillaryRequirements() == null
                    || value.ancillaryRequirements().packaging() == null
                    || !value.ancillaryRequirements().packaging().createsServiceStep()) {
                throw invalid("AI_INTENT_ANCILLARY_ONLY_INVALID", "仅附加工艺必须只包含可保存的包装附加工艺");
            }
            if (value.processMode() != null && !"SERVICE_ONLY".equals(value.processMode())) {
                throw invalid("AI_INTENT_PROCESS_MODE_INVALID", "仅附加工艺必须选择仅附加工艺方式");
            }
            return;
        }
        if ("DIRECT_SHIP".equals(value.processType())) {
            if (value.rewindIntent() != null || value.sawIntent() != null
                    || value.ancillaryRequirements() != null) {
                throw invalid("AI_INTENT_DIRECT_SHIP_INVALID", "不加工直发不能同时配置主工艺或附加工艺");
            }
            if (value.processMode() != null && !"DIRECT_SHIP".equals(value.processMode())) {
                throw invalid("AI_INTENT_PROCESS_MODE_INVALID", "不加工直发必须选择直发方式");
            }
            return;
        }
        boolean rewind = "REWIND".equals(value.processType());
        boolean invalidRewind = rewind && (value.rewindIntent() == null || value.sawIntent() != null);
        boolean invalidSaw = !rewind && (value.sawIntent() == null || value.rewindIntent() != null);
        if (invalidRewind || invalidSaw) {
            throw invalid("AI_INTENT_PROCESS_MISMATCH", "AI解析的加工类型与工艺意图不一致");
        }
        if (!"STANDARD".equals(effectiveProcessMode(value))
                && !"ON_SITE".equals(effectiveProcessMode(value))) {
            throw invalid("AI_INTENT_PROCESS_MODE_INVALID", "锯纸或复卷只能选择标准加工或现场定尺");
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

    private boolean isServiceOnly(ProcessAiAssignment value) {
        return "SERVICE_ONLY".equals(value.processType())
                || "ANCILLARY_ONLY".equals(value.processType());
    }

    private String effectiveProcessMode(ProcessAiAssignment value) {
        if (value.processMode() != null) return value.processMode();
        if (isServiceOnly(value)) return "SERVICE_ONLY";
        if ("DIRECT_SHIP".equals(value.processType())) return "DIRECT_SHIP";
        return "STANDARD";
    }

    private void validateCustomerSpecs(java.util.List<ProcessAiCustomerSpec> specs) {
        Set<Integer> indexes = new HashSet<>();
        for (ProcessAiCustomerSpec spec : specs) {
            if (spec.outputIndex() == null || !indexes.add(spec.outputIndex())) {
                throw invalid("AI_CUSTOMER_SPEC_INDEX_INVALID", "客户销售规格排布序号重复或缺失");
            }
            if (!spec.hasValue()) {
                throw invalid("AI_CUSTOMER_SPEC_EMPTY", "客户销售规格至少需要填写一个字段");
            }
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
