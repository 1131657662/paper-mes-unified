package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Verifies model evidence against server-owned, allowlisted facts before compilation. */
@Component
@RequiredArgsConstructor
public class ProcessAiEvidenceVerifier {

    private static final String MODEL_SOURCE = "model-inference";
    private static final Set<String> DEFAULT_IDS = Set.of("REWIND_FINISH_CORE_3_INCH");
    private static final Set<String> CUSTOMER_REFS = Set.of("customerRequirement", "customerText");
    private static final Set<String> FACT_FIELDS = Set.of(
            "widthMm", "originalWidth", "originalDiameter", "coreDiameter",
            "pieceCount", "gramWeight");
    private static final Pattern FACT_REF = Pattern.compile("R[1-9]\\d{0,2}\\.[A-Za-z][A-Za-z0-9_]{0,63}");

    private final ProcessTextRedactor redactor;

    public ProcessAiExtractionResult verify(ProcessAiExtractionResult extraction,
                                             String customerRequirement,
                                             ProcessAiOrderContext order,
                                             List<String> memoryItemIds) {
        boolean unverified = false;
        List<ProcessAiAssignment> assignments = new ArrayList<>();
        for (ProcessAiAssignment assignment : extraction.assignments()) {
            List<ProcessAiEvidence> evidence = new ArrayList<>();
            for (ProcessAiEvidence item : assignment.evidence()) {
                ProcessAiEvidence verified = verify(item, customerRequirement, order, memoryItemIds);
                unverified |= "MODEL_INFERENCE".equals(verified.sourceType());
                evidence.add(verified);
            }
            appendCustomerSpecEvidence(assignment.customerSpecs(), evidence, customerRequirement);
            assignments.add(new ProcessAiAssignment(assignment.sourceRollRefs(),
                    assignment.ownerRollRef(), assignment.coveredRollRefs(), assignment.processType(),
                    assignment.processMode(), assignment.rewindIntent(), assignment.sawIntent(),
                    assignment.ancillaryRequirements(), evidence, assignment.customerSpecs()));
        }
        if (!unverified && assignments.equals(extraction.assignments())) return extraction;
        List<String> questions = extraction.clarificationQuestions();
        return new ProcessAiExtractionResult(extraction.parseId(), extraction.schemaVersion(), assignments,
                extraction.unmappedText(), extraction.conflicts(), extraction.needsClarification() || unverified,
                questions);
    }

    private ProcessAiEvidence verify(ProcessAiEvidence item, String customerRequirement,
                                     ProcessAiOrderContext order, List<String> memoryItemIds) {
        String text = redactor.redact(item.text()).sanitizedText();
        if (text.isBlank()) return inference(item.field(), "[证据已隐藏]");
        String source = item.sourceType();
        String ref = item.sourceRef();
        boolean verified = source != null && switch (source) {
            case "CUSTOMER_TEXT" -> CUSTOMER_REFS.contains(ref)
                    && ProcessAiEvidenceTextMatcher.contains(customerRequirement, text);
            case "DB_FACT" -> factVerified(ref, text, order);
            case "APPROVED_MEMORY" -> memoryItemIds != null && memoryItemIds.contains(ref);
            case "DEFAULT" -> DEFAULT_IDS.contains(ref);
            default -> false;
        };
        if (!verified && (source == null || source.isBlank())
                && ProcessAiEvidenceTextMatcher.contains(customerRequirement, text)) {
            return new ProcessAiEvidence(item.field(), text, "CUSTOMER_TEXT", "customerRequirement");
        }
        return verified ? new ProcessAiEvidence(item.field(), text, source, ref)
                : inference(item.field(), text);
    }

    private boolean factVerified(String sourceRef, String text, ProcessAiOrderContext order) {
        if (sourceRef == null || !FACT_REF.matcher(sourceRef).matches()) return false;
        int separator = sourceRef.indexOf('.');
        String rollRef = sourceRef.substring(0, separator);
        String field = sourceRef.substring(separator + 1);
        if (!FACT_FIELDS.contains(field) || order == null) return false;
        return order.rolls().stream().filter(roll -> rollRef.equals(roll.shortRef()))
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

    private boolean factMatches(String text, String expected) {
        return ProcessAiEvidenceTextMatcher.contains(text, expected);
    }

    private String value(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private ProcessAiEvidence inference(String field, String text) {
        return new ProcessAiEvidence(field, text, "MODEL_INFERENCE", MODEL_SOURCE);
    }

    private void appendCustomerSpecEvidence(List<ProcessAiCustomerSpec> specs,
                                             List<ProcessAiEvidence> evidence,
                                             String customerRequirement) {
        if (specs == null || specs.isEmpty() || customerRequirement == null) return;
        for (ProcessAiCustomerSpec spec : specs) {
            appendCustomerSpecValue(evidence, customerRequirement,
                    "customerSpecs.paperName", spec.paperName());
            appendCustomerSpecValue(evidence, customerRequirement,
                    "customerSpecs.gramWeight", spec.gramWeight() == null
                            ? null : String.valueOf(spec.gramWeight()));
            appendCustomerSpecValue(evidence, customerRequirement,
                    "customerSpecs.finishWidth", spec.finishWidth() == null
                            ? null : String.valueOf(spec.finishWidth()));
        }
    }

    private void appendCustomerSpecValue(List<ProcessAiEvidence> evidence,
                                         String customerRequirement, String field, String value) {
        if (value == null || value.isBlank()) return;
        String sanitized = redactor.redact(value).sanitizedText();
        if (sanitized.isBlank() || !ProcessAiEvidenceTextMatcher.contains(customerRequirement, sanitized)) {
            return;
        }
        boolean alreadyVerified = evidence.stream().anyMatch(item ->
                isTrustedCustomerEvidence(item) &&
                        ProcessAiEvidenceTextMatcher.contains(item.text(), sanitized));
        if (!alreadyVerified) {
            evidence.add(new ProcessAiEvidence(field, sanitized, "CUSTOMER_TEXT",
                    "customerRequirement"));
        }
    }

    private boolean isTrustedCustomerEvidence(ProcessAiEvidence item) {
        return "CUSTOMER_TEXT".equals(item.sourceType())
                && "customerRequirement".equals(item.sourceRef());
    }
}
