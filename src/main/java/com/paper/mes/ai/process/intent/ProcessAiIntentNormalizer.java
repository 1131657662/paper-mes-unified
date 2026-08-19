package com.paper.mes.ai.process.intent;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ProcessAiIntentNormalizer {

    private static final Pattern WIDTH_SPLIT_IN_TWO = Pattern.compile(
            "(?:门幅|宽幅)\\s*(?:一\\s*分\\s*(?:为\\s*)?[二两2]"
                    + "|分\\s*(?:为|成)?\\s*[二两2](?:份|件)?)");
    private static final Pattern EXPLICIT_DIAMETER = Pattern.compile(
            "(?:目标|成品)?\\s*直径\\s*[:：=]?\\s*"
                    + "(\\d+(?:\\.\\d+)?)\\s*(mm|毫米|inch|英寸)",
            Pattern.CASE_INSENSITIVE);

    public ProcessAiExtractionResult normalize(ProcessAiExtractionResult result,
                                                String currentRequirement) {
        List<ProcessAiAssignment> assignments = result.assignments().stream()
                .map(value -> normalizeAssignment(value, currentRequirement))
                .toList();
        if (assignments.equals(result.assignments())) return result;
        return new ProcessAiExtractionResult(
                result.parseId(), result.schemaVersion(), assignments,
                result.unmappedText(), result.conflicts(), result.needsClarification(),
                result.clarificationQuestions());
    }

    private ProcessAiAssignment normalizeAssignment(ProcessAiAssignment value,
                                                    String currentRequirement) {
        if (value.rewindIntent() == null) return value;
        ProcessAiRewindIntent rewind = value.rewindIntent();
        ProcessAiMeasurement explicitDiameter = explicitDiameter(value.evidence());
        boolean widthSplit = hasTrustedWidthSplitEvidence(value.evidence(), currentRequirement)
                || hasExplicitWidthSplit(rewind.widthRule());
        boolean diameterChange = explicitDiameter != null || hasExplicitDiameter(rewind);
        ProcessAiRewindIntent normalized = new ProcessAiRewindIntent(
                normalizedMode(rewind, widthSplit, diameterChange),
                withExplicitDiameter(rewind.diameterRule(), explicitDiameter), rewind.core(),
                widthSplit && hasTrustedWidthSplitEvidence(value.evidence(), currentRequirement)
                        ? new ProcessAiWidthRule("KNIFE_COUNT", null, "mm", 1)
                        : rewind.widthRule());
        if (normalized.equals(rewind)) return value;
        return new ProcessAiAssignment(
                value.sourceRollRefs(), value.ownerRollRef(), value.coveredRollRefs(),
                value.processType(), normalized, value.sawIntent(),
                value.ancillaryRequirements(), value.evidence());
    }

    private ProcessAiDiameterRule withExplicitDiameter(ProcessAiDiameterRule rule,
                                                       ProcessAiMeasurement explicit) {
        if (explicit == null) return rule;
        if (rule == null) {
            return new ProcessAiDiameterRule("EXPLICIT", 1,
                    List.of(BigDecimal.valueOf(100)), explicit);
        }
        if (rule.targetDiameter() == null) {
            return new ProcessAiDiameterRule(rule.type(), rule.parts(), rule.ratios(), explicit);
        }
        if (!"EXPLICIT".equals(rule.targetDiameter().source())
                && sameMeasurement(rule.targetDiameter(), explicit)) {
            return new ProcessAiDiameterRule(rule.type(), rule.parts(), rule.ratios(), explicit);
        }
        return rule;
    }

    private boolean sameMeasurement(ProcessAiMeasurement left, ProcessAiMeasurement right) {
        return left.value().compareTo(right.value()) == 0
                && normalizeUnit(left.unit()).equals(normalizeUnit(right.unit()));
    }

    private ProcessAiMeasurement explicitDiameter(List<ProcessAiEvidence> evidence) {
        return evidence.stream()
                .map(ProcessAiEvidence::text)
                .map(EXPLICIT_DIAMETER::matcher)
                .filter(matcher -> matcher.find())
                .findFirst()
                .map(matcher -> new ProcessAiMeasurement(
                        new BigDecimal(matcher.group(1)), normalizeUnit(matcher.group(2)), "EXPLICIT"))
                .orElse(null);
    }

    private String normalizeUnit(String unit) {
        return switch (unit.toLowerCase()) {
            case "毫米" -> "mm";
            case "英寸" -> "inch";
            default -> unit.toLowerCase();
        };
    }

    private boolean hasExplicitWidthSplit(ProcessAiWidthRule rule) {
        return rule != null && "EXPLICIT".equals(rule.type())
                && rule.values() != null && rule.values().size() > 1;
    }

    private boolean hasExplicitDiameter(ProcessAiRewindIntent rewind) {
        return rewind.diameterRule() != null
                && (("WEIGHT_SPLIT".equals(rewind.diameterRule().type())
                && !"CHANGE_WIDTH".equals(rewind.modeIntent()))
                || (rewind.diameterRule().targetDiameter() != null
                && "EXPLICIT".equals(rewind.diameterRule().targetDiameter().source())));
    }

    private boolean hasTrustedWidthSplitEvidence(List<ProcessAiEvidence> evidence,
                                                 String currentRequirement) {
        if (currentRequirement == null || currentRequirement.isBlank()) return false;
        return evidence.stream().anyMatch(value -> "widthRule".equals(value.field())
                && currentRequirement.contains(value.text())
                && WIDTH_SPLIT_IN_TWO.matcher(value.text()).find());
    }

    private String normalizedMode(ProcessAiRewindIntent rewind, boolean widthSplit,
                                  boolean diameterChange) {
        if (widthSplit && diameterChange) return "CHANGE_WIDTH_AND_DIAMETER";
        if (widthSplit) return "CHANGE_WIDTH";
        if (diameterChange && "CHANGE_WIDTH".equals(rewind.modeIntent())) {
            return "CHANGE_DIAMETER";
        }
        return switch (rewind.modeIntent()) {
            case "CHANGE_DIAMETER" -> "CHANGE_DIAMETER";
            case "KEEP_SPEC" -> "KEEP_SPEC";
            default -> rewind.modeIntent();
        };
    }
}
