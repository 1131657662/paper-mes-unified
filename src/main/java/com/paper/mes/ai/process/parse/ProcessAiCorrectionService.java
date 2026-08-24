package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiCustomerSpec;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiMeasurement;
import com.paper.mes.ai.process.intent.ProcessAiQuantityIntent;
import com.paper.mes.ai.process.intent.ProcessAiRewindIntent;
import com.paper.mes.ai.process.intent.ProcessAiWidthRule;
import com.paper.mes.ai.process.parse.dto.ProcessAiCorrection;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Applies a bounded correction vocabulary to an extraction without accepting JSON pointers. */
@Component
public class ProcessAiCorrectionService {

    private static final BigDecimal MAX_MEASUREMENT = new BigDecimal("10000");

    public ProcessAiExtractionResult apply(ProcessAiExtractionResult extraction,
                                           List<ProcessAiCorrection> corrections) {
        List<ProcessAiAssignment> assignments = new ArrayList<>(extraction.assignments());
        List<String> clarificationQuestions = new ArrayList<>(extraction.clarificationQuestions());
        for (ProcessAiCorrection correction : corrections) {
            int index = findAssignment(assignments, correction.assignmentRef());
            ProcessAiAssignment assignment = assignments.get(index);
            assignments.set(index, applyToAssignment(assignment, correction));
            clarificationQuestions.removeIf(question -> resolvedBy(correction, question));
        }
        boolean blocked = !clarificationQuestions.isEmpty()
                || !extraction.unmappedText().isEmpty()
                || !extraction.conflicts().isEmpty();
        return new ProcessAiExtractionResult(extraction.parseId(), extraction.schemaVersion(),
                assignments, extraction.unmappedText(), extraction.conflicts(), blocked,
                clarificationQuestions);
    }

    private boolean resolvedBy(ProcessAiCorrection correction, String question) {
        String text = question == null ? "" : question;
        return ("quantityScope".equals(correction.field())
                && (text.contains("数量") || text.contains("每条") || text.contains("全单")))
                || ("widthMm".equals(correction.field()) && text.contains("门幅"))
                || ("finishCoreDiameter".equals(correction.field()) &&
                (text.contains("纸芯") || text.contains("芯径")));
    }

    private ProcessAiAssignment applyToAssignment(ProcessAiAssignment assignment,
                                                  ProcessAiCorrection correction) {
        if (isCustomerSpecField(correction.field())) {
            return applyCustomerSpec(assignment, correction);
        }
        if (assignment.rewindIntent() == null) {
            throw invalid("AI_CORRECTION_FIELD_INVALID", "该字段只适用于复卷工艺");
        }
        ProcessAiRewindIntent rewind = assignment.rewindIntent();
        ProcessAiRewindIntent revised = switch (correction.field()) {
            case "finishCoreDiameter" -> new ProcessAiRewindIntent(
                    rewind.modeIntent(), rewind.diameterRule(), measurement(correction),
                    rewind.widthRule(), rewind.quantityIntent());
            case "widthMm" -> new ProcessAiRewindIntent(
                    rewind.modeIntent(), rewind.diameterRule(), rewind.core(),
                    new ProcessAiWidthRule("EXPLICIT", List.of(integerMm(correction, "门幅")),
                            "mm", null), rewind.quantityIntent());
            case "quantityScope" -> new ProcessAiRewindIntent(
                    rewind.modeIntent(), rewind.diameterRule(), rewind.core(), rewind.widthRule(),
                    quantityScope(rewind.quantityIntent(), correction));
            default -> throw invalid("AI_CORRECTION_FIELD_INVALID", "不允许修改该工艺字段");
        };
        return new ProcessAiAssignment(assignment.sourceRollRefs(), assignment.ownerRollRef(),
                assignment.coveredRollRefs(), assignment.processType(), assignment.processMode(), revised,
                assignment.sawIntent(), assignment.ancillaryRequirements(), assignment.evidence(),
                assignment.customerSpecs());
    }

    private boolean isCustomerSpecField(String field) {
        return field.startsWith("customer");
    }

    private ProcessAiAssignment applyCustomerSpec(ProcessAiAssignment assignment,
                                                  ProcessAiCorrection correction) {
        Integer index = correction.outputIndex();
        if (index == null) throw invalid("AI_CORRECTION_OUTPUT_INVALID", "客户销售规格缺少成品序号");
        List<ProcessAiCustomerSpec> specs = new ArrayList<>(assignment.customerSpecs());
        int existing = -1;
        for (int i = 0; i < specs.size(); i++) {
            if (index.equals(specs.get(i).outputIndex())) {
                existing = i;
                break;
            }
        }
        ProcessAiCustomerSpec current = existing < 0
                ? new ProcessAiCustomerSpec(index, null, null, null, null) : specs.get(existing);
        ProcessAiCustomerSpec revised = switch (correction.field()) {
            case "customerPaperName" -> new ProcessAiCustomerSpec(index,
                    requiredText(correction, "客户品名"), current.gramWeight(),
                    current.finishWidth(), current.overrideReason());
            case "customerGramWeight" -> new ProcessAiCustomerSpec(index, current.paperName(),
                    integerValue(correction, "客户克重"), current.finishWidth(), current.overrideReason());
            case "customerFinishWidth" -> new ProcessAiCustomerSpec(index, current.paperName(),
                    current.gramWeight(), integerMm(correction, "客户门幅"), current.overrideReason());
            case "customerSpecOverrideReason" -> new ProcessAiCustomerSpec(index, current.paperName(),
                    current.gramWeight(), current.finishWidth(), requiredText(correction, "改写原因"));
            default -> throw invalid("AI_CORRECTION_FIELD_INVALID", "不允许修改该客户销售字段");
        };
        if (existing < 0) specs.add(revised); else specs.set(existing, revised);
        return new ProcessAiAssignment(assignment.sourceRollRefs(), assignment.ownerRollRef(),
                assignment.coveredRollRefs(), assignment.processType(), assignment.processMode(), assignment.rewindIntent(),
                assignment.sawIntent(), assignment.ancillaryRequirements(), assignment.evidence(), specs);
    }

    private String requiredText(ProcessAiCorrection correction, String label) {
        if (correction.textValue() == null || correction.textValue().isBlank()) {
            throw invalid("AI_CORRECTION_VALUE_INVALID", label + "不能为空");
        }
        return correction.textValue().trim();
    }

    private int integerValue(ProcessAiCorrection correction, String label) {
        if (correction.unit() != null) {
            throw invalid("AI_CORRECTION_UNIT_INVALID", label + "不需要单位");
        }
        int value = integer(correction, label);
        if (value > 10000) throw invalid("AI_CORRECTION_VALUE_INVALID", label + "不能超过10000");
        return value;
    }

    private ProcessAiMeasurement measurement(ProcessAiCorrection correction) {
        BigDecimal value = requiredValue(correction, "纸芯");
        if (value.compareTo(MAX_MEASUREMENT) > 0) {
            throw invalid("AI_CORRECTION_VALUE_INVALID", "纸芯不能超过10000");
        }
        String unit = correction.unit() == null ? "inch" : correction.unit();
        if (!"inch".equals(unit) && !"mm".equals(unit)) {
            throw invalid("AI_CORRECTION_UNIT_INVALID", "纸芯单位无效");
        }
        return new ProcessAiMeasurement(value, unit, "EXPLICIT");
    }

    private ProcessAiQuantityIntent quantityScope(ProcessAiQuantityIntent quantity,
                                                   ProcessAiCorrection correction) {
        if (quantity == null || correction.textValue() == null
                || (!"PER_SOURCE".equals(correction.textValue())
                && !"TOTAL".equals(correction.textValue()))) {
            throw invalid("AI_CORRECTION_QUANTITY_INVALID", "数量范围修正无效");
        }
        String scope = correction.textValue();
        // The allocation table has different invariants for each scope.  When a
        // correction changes scope, discard the old table instead of carrying an
        // impossible TOTAL/PER_SOURCE combination into compilation.
        List<com.paper.mes.ai.process.intent.ProcessAiSourceAllocation> allocations =
                scope.equals(quantity.scope()) ? quantity.sourceAllocation() : List.of();
        return new ProcessAiQuantityIntent(quantity.type(), quantity.widthMm(), quantity.count(),
                scope, allocations);
    }

    private int integer(ProcessAiCorrection correction, String label) {
        try {
            return requiredValue(correction, label).stripTrailingZeros().intValueExact();
        } catch (ArithmeticException ex) {
            throw invalid("AI_CORRECTION_VALUE_INVALID", label + "必须为整数毫米");
        }
    }

    private int integerMm(ProcessAiCorrection correction, String label) {
        if (!"mm".equals(correction.unit())) {
            throw invalid("AI_CORRECTION_UNIT_INVALID", label + "单位必须为mm");
        }
        int value = integer(correction, label);
        if (value > 10000) {
            throw invalid("AI_CORRECTION_VALUE_INVALID", label + "不能超过10000毫米");
        }
        return value;
    }

    private BigDecimal requiredValue(ProcessAiCorrection correction, String label) {
        if (correction.value() == null || correction.value().signum() <= 0) {
            throw invalid("AI_CORRECTION_VALUE_INVALID", label + "必须大于0");
        }
        return correction.value();
    }

    private int findAssignment(List<ProcessAiAssignment> assignments, String ownerRef) {
        for (int index = 0; index < assignments.size(); index++) {
            if (assignments.get(index).ownerRollRef().equals(ownerRef)) return index;
        }
        throw invalid("AI_CORRECTION_ASSIGNMENT_INVALID", "修正引用了当前解析不存在的母卷");
    }

    private BusinessException invalid(String code, String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, code, message);
    }
}
