package com.paper.mes.ai.process.intent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class ProcessAiIntentNormalizer {

    private static final Pattern WIDTH_SPLIT_IN_TWO = Pattern.compile(
            "(?:门幅|宽幅)\\s*(?:一\\s*分\\s*(?:为\\s*)?[二两2]"
                    + "|分\\s*(?:为|成)?\\s*[二两2](?:份|件)?)");

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
        if (value.rewindIntent() == null || !hasTrustedWidthSplitEvidence(
                value.evidence(), currentRequirement)) return value;
        ProcessAiRewindIntent rewind = value.rewindIntent();
        ProcessAiRewindIntent normalized = new ProcessAiRewindIntent(
                normalizedMode(rewind), rewind.diameterRule(), rewind.core(),
                new ProcessAiWidthRule("KNIFE_COUNT", null, "mm", 1));
        return new ProcessAiAssignment(
                value.sourceRollRefs(), value.ownerRollRef(), value.coveredRollRefs(),
                value.processType(), normalized, value.sawIntent(),
                value.ancillaryRequirements(), value.evidence());
    }

    private boolean hasTrustedWidthSplitEvidence(List<ProcessAiEvidence> evidence,
                                                 String currentRequirement) {
        if (currentRequirement == null || currentRequirement.isBlank()) return false;
        return evidence.stream().anyMatch(value -> "widthRule".equals(value.field())
                && currentRequirement.contains(value.text())
                && WIDTH_SPLIT_IN_TWO.matcher(value.text()).find());
    }

    private String normalizedMode(ProcessAiRewindIntent rewind) {
        return switch (rewind.modeIntent()) {
            case "CHANGE_DIAMETER" -> "CHANGE_WIDTH_AND_DIAMETER";
            case "KEEP_SPEC" -> "CHANGE_WIDTH";
            default -> rewind.modeIntent();
        };
    }
}
