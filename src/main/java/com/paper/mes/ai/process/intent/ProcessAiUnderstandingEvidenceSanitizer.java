package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Re-labels model evidence only after checking it against allowlisted facts. */
@Component
@RequiredArgsConstructor
public class ProcessAiUnderstandingEvidenceSanitizer {

    private static final String MODEL_SOURCE = "model-inference";
    private static final Set<String> DEFAULT_IDS = Set.of("REWIND_FINISH_CORE_3_INCH");
    private static final Set<String> CUSTOMER_REFS = Set.of("customerText", "customerRequirement");
    private static final Set<String> FACT_FIELDS = Set.of(
            "widthMm", "originalWidth", "originalDiameter", "coreDiameter", "pieceCount", "gramWeight");
    private static final Pattern FACT_REF = Pattern.compile("R[1-9]\\d{0,2}\\.[A-Za-z][A-Za-z0-9_]{0,63}");

    private final ProcessTextRedactor redactor;

    public ProcessAiUnderstandingResult sanitize(ProcessAiUnderstandingResult result,
                                                 String customerText,
                                                 ProcessAiOrderContext order,
                                                 List<String> memoryItemIds) {
        List<ProcessAiUnderstandingEvidence> evidence = result.evidence().stream()
                .map(item -> sanitize(item, customerText, order, memoryItemIds))
                .toList();
        return new ProcessAiUnderstandingResult(result.parseId(), result.schemaVersion(),
                result.conclusion(), evidence, result.assumptions(), result.risks(),
                result.clarificationQuestions(), result.needsClarification());
    }

    private ProcessAiUnderstandingEvidence sanitize(ProcessAiUnderstandingEvidence item,
                                                    String customerText,
                                                    ProcessAiOrderContext order,
                                                    List<String> memoryItemIds) {
        String text = redactor.redact(item.text()).sanitizedText();
        String range = item.normalizedRange() == null ? null
                : redactor.redact(item.normalizedRange()).sanitizedText();
        if (text.isBlank()) return modelInference(item.field(), "[证据已隐藏]");
        boolean verified = switch (item.sourceType()) {
            case "CUSTOMER_TEXT" -> CUSTOMER_REFS.contains(item.sourceRef())
                    && ProcessAiEvidenceTextMatcher.contains(customerText, text)
                    && (range == null || ProcessAiEvidenceTextMatcher.contains(customerText, range));
            case "DB_FACT" -> factVerified(item, text, order);
            case "APPROVED_MEMORY" -> memoryItemIds.contains(item.sourceRef());
            case "DEFAULT" -> DEFAULT_IDS.contains(item.sourceRef());
            case "MODEL_INFERENCE" -> false;
            default -> false;
        };
        return verified ? new ProcessAiUnderstandingEvidence(item.field(), text,
                item.sourceType(), item.sourceRef(), range)
                : modelInference(item.field(), text);
    }

    private boolean factVerified(ProcessAiUnderstandingEvidence item, String text,
                                 ProcessAiOrderContext order) {
        if (!FACT_REF.matcher(item.sourceRef()).matches()) return false;
        int separator = item.sourceRef().indexOf('.');
        String ref = item.sourceRef().substring(0, separator);
        String field = item.sourceRef().substring(separator + 1);
        if (!FACT_FIELDS.contains(field)) return false;
        return order.rolls().stream().filter(roll -> roll.shortRef().equals(ref))
                .map(roll -> expectedFact(roll, field))
                .anyMatch(expected -> expected != null && factMatches(text, expected));
    }

    private String expectedFact(ProcessAiRollContext roll, String field) {
        return switch (field) {
            case "widthMm", "originalWidth" -> value(roll.originalWidth());
            case "originalDiameter" -> value(roll.originalDiameter());
            case "coreDiameter" -> value(roll.coreDiameter());
            case "pieceCount" -> value(roll.pieceNum());
            case "gramWeight" -> value(roll.gramWeight());
            default -> null;
        };
    }

    private String value(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean factMatches(String text, String expected) {
        return ProcessAiEvidenceTextMatcher.contains(text, expected);
    }

    private ProcessAiUnderstandingEvidence modelInference(String field, String text) {
        return new ProcessAiUnderstandingEvidence(field, text, "MODEL_INFERENCE", MODEL_SOURCE, null);
    }
}
