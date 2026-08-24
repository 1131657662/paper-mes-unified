package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.memory.ProjectMemoryContextSelector;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiMeasurement;
import com.paper.mes.ai.process.intent.ProcessAiRewindIntent;
import com.paper.mes.ai.process.intent.ProcessAiSawIntent;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds learning candidates only from fields the operator explicitly accepted. */
@Component
@RequiredArgsConstructor
class ProjectMemoryCandidateExtractor {

    private final ObjectMapper objectMapper;
    private final ProjectMemoryContextSelector memorySelector;
    private final ProcessTextRedactor redactor;

    List<ProjectMemoryCandidateProposal> extract(ProcessAiExtractionResult extraction,
                                                 List<String> acceptedPaths,
                                                 ProjectMemorySnapshot memory) {
        List<ProjectMemoryCandidateProposal> result = new ArrayList<>();
        for (ProcessAiAssignment assignment : extraction.assignments()) {
            if (containsUnsafeOrKnownEvidence(assignment, memory)) continue;
            String prefix = "/assignments/" + assignment.ownerRollRef() + "/";
            acceptedPaths.stream().filter(path -> path.startsWith(prefix))
                    .map(path -> path.substring(prefix.length())).filter(this::learnable)
                    .distinct().map(path -> proposal(assignment, path, memory))
                    .flatMap(java.util.Optional::stream).forEach(result::add);
        }
        return result;
    }

    private boolean containsUnsafeOrKnownEvidence(ProcessAiAssignment assignment,
                                                  ProjectMemorySnapshot memory) {
        return assignment.evidence().stream().anyMatch(item -> {
            String text = item.text() == null ? "" : item.text().replaceAll("\\s+", " ").trim();
            if (text.isBlank()) return true;
            if (redactor.redact(text).modified()) return true;
            return !memorySelector.selectWithIds(memory, text,
                    "process-memory-candidate", 2_000).itemIds().isEmpty();
        });
    }

    private java.util.Optional<ProjectMemoryCandidateProposal> proposal(
            ProcessAiAssignment assignment, String fieldPath, ProjectMemorySnapshot memory) {
        String value = structuredValue(assignment, fieldPath);
        if (value == null) return java.util.Optional.empty();
        String canonical = normalizeCanonical(fieldPath + "=" + value);
        if (canonical == null) return java.util.Optional.empty();
        String type = canonical.matches(".*\\d.*|.*\\[.*") ? "EXAMPLE" : "TERM";
        String scope = scope(assignment, fieldPath);
        String intent = intent(assignment, fieldPath);
        if (known(memory, canonical, type)) return java.util.Optional.empty();
        ObjectNode document = document(type, scope, intent, canonical, fieldPath,
                assignment.processType());
        String hash = ProcessAiAuditHasher.sha256(type, canonical.toLowerCase(Locale.ROOT));
        return java.util.Optional.of(new ProjectMemoryCandidateProposal(
                type.toLowerCase(Locale.ROOT) + "-candidate-" + hash.substring(0, 24),
                type, scope, intent, canonical, document, assignment));
    }

    private ObjectNode document(String type, String scope, String intent,
                                String canonical, String fieldPath, String processType) {
        ObjectNode document = objectMapper.createObjectNode();
        document.put("type", type);
        document.put("scope", scope);
        document.put("status", "ACTIVE");
        if ("TERM".equals(type)) {
            document.put("phrase", canonical);
            document.putArray("aliases");
            document.put("intent", intent);
            document.put("meaning", "已确认结构化字段");
        } else {
            document.put("input", canonical);
            document.putObject("expected")
                    .put("processType", processType)
                    .put("intent", intent)
                    .put("field", fieldPath);
            document.put("evidenceRequired", true);
        }
        document.put("source", "confirmed-ai-candidate");
        return document;
    }

    private boolean learnable(String fieldPath) {
        return !fieldPath.equals("processType") && !fieldPath.equals("sourceRollRefs")
                && !fieldPath.equals("coveredRollRefs") && !fieldPath.equals("machineUuid");
    }

    private String structuredValue(ProcessAiAssignment assignment, String path) {
        if (path.equals("processType")) return assignment.processType();
        if (path.equals("rewindIntent/modeIntent")) return assignment.rewindIntent() == null
                ? null : assignment.rewindIntent().modeIntent();
        if (path.startsWith("rewindIntent/")) return rewindValue(assignment.rewindIntent(),
                path.substring("rewindIntent/".length()));
        if (path.startsWith("sawIntent/")) return sawValue(assignment.sawIntent(),
                path.substring("sawIntent/".length()));
        if (path.equals("ancillaryRequirements/label")) return assignment.ancillaryRequirements() == null
                || assignment.ancillaryRequirements().label() == null ? null : "LABEL_REQUIRED";
        if (path.equals("ancillaryRequirements/packaging")) return assignment.ancillaryRequirements() == null
                || assignment.ancillaryRequirements().packaging() == null ? null : "PACKAGING_REQUIRED";
        return null;
    }

    private String rewindValue(ProcessAiRewindIntent intent, String path) {
        if (intent == null) return null;
        if (path.equals("core")) return measurement(intent.core());
        if (path.startsWith("diameterRule/")) {
            if (intent.diameterRule() == null) return null;
            if (path.endsWith("type")) return intent.diameterRule().type();
            if (path.endsWith("parts")) return String.valueOf(intent.diameterRule().parts());
            if (path.endsWith("ratios")) return String.valueOf(intent.diameterRule().ratios());
            if (path.endsWith("targetDiameter")) return measurement(intent.diameterRule().targetDiameter());
        }
        if (path.startsWith("widthRule/")) {
            if (intent.widthRule() == null) return null;
            if (path.endsWith("type")) return intent.widthRule().type();
            if (path.endsWith("values")) return String.valueOf(intent.widthRule().values());
            if (path.endsWith("knifeCount")) return String.valueOf(intent.widthRule().knifeCount());
        }
        return null;
    }

    private String sawValue(ProcessAiSawIntent intent, String path) {
        if (intent == null) return null;
        if (path.equals("type")) return intent.type();
        if (path.equals("knifeCount")) return String.valueOf(intent.knifeCount());
        if (path.equals("widths")) return String.valueOf(intent.widths());
        return null;
    }

    private String measurement(ProcessAiMeasurement value) {
        return value == null ? null : value.value() + value.unit() + ":" + value.source();
    }

    private String normalizeCanonical(String value) {
        if (value == null || value.length() < 2 || value.length() > 200) return null;
        var redaction = redactor.redact(value);
        return redaction.modified() ? null : redaction.sanitizedText();
    }

    private boolean known(ProjectMemorySnapshot memory, String phrase, String type) {
        if ("EXAMPLE".equals(type)) {
            JsonNode examples = memory.document().path("examples");
            if (!examples.isObject()) return false;
            var fields = examples.fields();
            while (fields.hasNext()) {
                JsonNode value = fields.next().getValue();
                if ("ACTIVE".equals(value.path("status").asText())
                        && phrase.equals(value.path("input").asText())) return true;
            }
            return false;
        }
        return !memorySelector.selectWithIds(memory, phrase,
                "process-memory-candidate", 2_000).itemIds().isEmpty();
    }

    private String scope(ProcessAiAssignment assignment, String field) {
        String normalized = field.toLowerCase(Locale.ROOT);
        if (normalized.contains("packag")) return "PACKAGING";
        if (normalized.contains("label")) return "ORDER_REQUIREMENT";
        if ("SAW".equals(assignment.processType())) return "SAW";
        return assignment.sourceRollRefs().size() > 1 ? "MULTI_SOURCE_REWIND" : "REWIND";
    }

    private String intent(ProcessAiAssignment assignment, String field) {
        String normalized = field.toLowerCase(Locale.ROOT);
        if (normalized.contains("packag")) return "REPACK";
        if (normalized.contains("label")) return "LABEL_ONLY";
        if (assignment.sawIntent() != null) return "SAW_" + assignment.sawIntent().type();
        if (normalized.contains("diameter") && assignment.rewindIntent() != null
                && assignment.rewindIntent().diameterRule() != null) {
            return assignment.rewindIntent().diameterRule().type();
        }
        if (normalized.contains("core")) return "CORE_DIAMETER";
        if (normalized.contains("width") && assignment.rewindIntent() != null
                && assignment.rewindIntent().widthRule() != null) {
            return "WIDTH_" + assignment.rewindIntent().widthRule().type();
        }
        return assignment.rewindIntent() == null ? assignment.processType()
                : assignment.rewindIntent().modeIntent();
    }
}
