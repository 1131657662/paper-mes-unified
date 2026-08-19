package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiEvidence;
import com.paper.mes.ai.process.intent.ProcessAiMeasurement;
import com.paper.mes.ai.process.intent.ProcessAiRewindIntent;
import com.paper.mes.ai.process.intent.ProcessAiWidthRule;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Blocks a READY candidate when its compiled fields contradict the model evidence. */
final class ProcessAiPlanEvidenceConsistencyGuard {

    private static final Pattern EXPLICIT_DIAMETER = Pattern.compile(
            "(?:目标|成品)?\\s*直径\\s*[:：=]?\\s*"
                    + "(\\d+(?:\\.\\d+)?)\\s*(mm|毫米|inch|英寸)",
            Pattern.CASE_INSENSITIVE);

    List<String> validate(ProcessAiAssignment assignment, ProcessPlanDTO plan) {
        if (!"REWIND".equals(assignment.processType()) || assignment.rewindIntent() == null) {
            return List.of();
        }
        List<String> errors = new ArrayList<>();
        ProcessAiRewindIntent intent = assignment.rewindIntent();
        Matcher diameter = explicitDiameter(assignment.evidence());
        if (diameter != null) validateDiameter(diameter, intent, plan, errors);
        if (hasWidthSplitEvidence(assignment.evidence(), intent.widthRule())) {
            validateWidthSplit(intent, plan, errors);
        }
        return errors;
    }

    private void validateDiameter(Matcher evidence, ProcessAiRewindIntent intent,
                                  ProcessPlanDTO plan, List<String> errors) {
        int mode = plan.getRewindMode() == null ? 0 : plan.getRewindMode();
        if (mode != 2 && mode != 3) {
            errors.add("证据与结构化结果冲突：目标直径未进入改直径模式");
            return;
        }
        List<RewindSegmentPlanDTO> segments = plan.getSegments() == null
                ? List.of() : plan.getSegments();
        if (segments.isEmpty() || segments.stream().anyMatch(item -> item.getTargetDiameter() == null)) {
            errors.add("证据与结构化结果冲突：目标直径未生成目标卷径");
            return;
        }
        ProcessAiMeasurement target = intent.diameterRule() == null
                ? null : intent.diameterRule().targetDiameter();
        if (target == null || !"EXPLICIT".equals(target.source())) {
            errors.add("证据与结构化结果冲突：目标直径没有进入结构化字段");
            return;
        }
        int expected = numericDiameter(evidence.group(1), evidence.group(2));
        if ("mm".equals(normalizeUnit(evidence.group(2)))
                && segments.stream().anyMatch(item -> item.getTargetDiameter() != expected)) {
            errors.add("证据与结构化结果冲突：目标直径数值不一致");
        }
    }

    private void validateWidthSplit(ProcessAiRewindIntent intent, ProcessPlanDTO plan,
                                    List<String> errors) {
        int mode = plan.getRewindMode() == null ? 0 : plan.getRewindMode();
        if (mode != 1 && mode != 3) {
            errors.add("证据与结构化结果冲突：成品门幅未进入改门幅模式");
            return;
        }
        ProcessAiWidthRule width = intent.widthRule();
        if (width == null || !"EXPLICIT".equals(width.type())
                || width.values() == null || width.values().size() < 2) {
            return;
        }
        List<RewindSegmentPlanDTO> segments = plan.getSegments() == null
                ? List.of() : plan.getSegments();
        if (segments.isEmpty()) return;
        if (segments.getFirst().getLayoutItems() == null) {
            errors.add("证据与结构化结果冲突：成品门幅排布缺失");
            return;
        }
        List<Integer> actual = segments.getFirst().getLayoutItems().stream()
                .filter(item -> "FINISH".equals(item.getItemType()))
                .map(RewindLayoutItemPlanDTO::getWidth)
                .toList();
        if (!actual.equals(width.values())) {
            errors.add("证据与结构化结果冲突：成品门幅排布不一致");
        }
    }

    private boolean hasWidthSplitEvidence(List<ProcessAiEvidence> evidence,
                                           ProcessAiWidthRule width) {
        if (width != null && "EXPLICIT".equals(width.type())
                && width.values() != null && width.values().size() > 1) return true;
        return evidence.stream().map(ProcessAiEvidence::text)
                .anyMatch(text -> text.contains("门幅") && (text.contains("+")
                        || text.contains("一分二") || text.contains("一分为二")));
    }

    private Matcher explicitDiameter(List<ProcessAiEvidence> evidence) {
        for (ProcessAiEvidence item : evidence) {
            Matcher matcher = EXPLICIT_DIAMETER.matcher(item.text());
            if (matcher.find()) return matcher;
        }
        return null;
    }

    private int numericDiameter(String value, String unit) {
        BigDecimal parsed = new BigDecimal(value);
        return parsed.intValue();
    }

    private String normalizeUnit(String unit) {
        return switch (unit.toLowerCase()) {
            case "毫米" -> "mm";
            case "英寸" -> "inch";
            default -> unit.toLowerCase();
        };
    }
}
