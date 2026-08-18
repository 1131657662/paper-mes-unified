package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.memory.ProjectMemoryContextSelector;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiEvidence;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
class ProjectMemoryCandidateExtractor {

    private static final int MAX_PHRASE_CHARS = 120;
    private static final Pattern ORDER_SPECIFIC = Pattern.compile(
            ".*(?:\\d\\s*(?:件|卷|mm|毫米|刀|%|英寸)|门幅\\s*\\d|直径\\s*\\d).*",
            Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final ProjectMemoryContextSelector memorySelector;
    private final ProcessTextRedactor redactor;

    List<ProjectMemoryCandidateProposal> extract(
            ProcessAiExtractionResult extraction,
            List<String> acceptedPaths,
            ProjectMemorySnapshot memory) {
        List<ProjectMemoryCandidateProposal> result = new ArrayList<>();
        for (ProcessAiAssignment assignment : extraction.assignments()) {
            if (!acceptedAssignment(assignment, acceptedPaths)) continue;
            for (ProcessAiEvidence evidence : assignment.evidence()) {
                proposal(assignment, evidence, memory).ifPresent(result::add);
            }
        }
        return result;
    }

    private java.util.Optional<ProjectMemoryCandidateProposal> proposal(
            ProcessAiAssignment assignment, ProcessAiEvidence evidence,
            ProjectMemorySnapshot memory) {
        String phrase = normalizedPhrase(evidence.text());
        if (phrase == null) return java.util.Optional.empty();
        String type = candidateType(phrase);
        if (known(memory, phrase, type)) return java.util.Optional.empty();
        String scope = scope(assignment, evidence.field());
        String intent = intent(assignment, evidence.field());
        ObjectNode document = "TERM".equals(type)
                ? termDocument(scope, intent, phrase, evidence.field())
                : exampleDocument(scope, intent, phrase, assignment, evidence.field());
        String hash = ProcessAiAuditHasher.sha256(
                type, phrase.toLowerCase(Locale.ROOT));
        return java.util.Optional.of(new ProjectMemoryCandidateProposal(
                type.toLowerCase(Locale.ROOT) + "-candidate-" + hash.substring(0, 24),
                type, scope, intent, phrase, document, assignment));
    }

    private ObjectNode termDocument(String scope, String intent, String phrase, String field) {
        ObjectNode document = objectMapper.createObjectNode();
        document.put("type", "TERM");
        document.put("scope", scope);
        document.put("status", "ACTIVE");
        document.put("phrase", phrase);
        document.putArray("aliases");
        document.put("intent", intent);
        document.put("meaning", "已确认的客户用语，对应字段：" + field);
        document.put("source", "confirmed-ai-candidate");
        return document;
    }

    private ObjectNode exampleDocument(String scope, String intent, String phrase,
                                       ProcessAiAssignment assignment, String field) {
        ObjectNode document = objectMapper.createObjectNode();
        document.put("type", "EXAMPLE");
        document.put("scope", scope);
        document.put("status", "ACTIVE");
        document.put("input", phrase);
        ObjectNode expected = document.putObject("expected");
        expected.put("processType", assignment.processType());
        expected.put("intent", intent);
        expected.put("field", field);
        document.put("evidenceRequired", true);
        document.put("source", "confirmed-ai-candidate");
        return document;
    }

    private String candidateType(String phrase) {
        return phrase.length() > 30 || ORDER_SPECIFIC.matcher(phrase).matches()
                ? "EXAMPLE" : "TERM";
    }

    private String normalizedPhrase(String value) {
        if (value == null) return null;
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() < 2 || normalized.length() > MAX_PHRASE_CHARS) return null;
        var redaction = redactor.redact(normalized);
        return redaction.modified() ? null : redaction.sanitizedText();
    }

    private boolean known(ProjectMemorySnapshot memory, String phrase, String type) {
        if ("EXAMPLE".equals(type)) {
            var examples = memory.document().path("examples");
            if (!examples.isObject()) return false;
            var fields = examples.fields();
            while (fields.hasNext()) {
                var value = fields.next().getValue();
                if ("ACTIVE".equals(value.path("status").asText())
                        && phrase.equals(value.path("input").asText())) return true;
            }
            return false;
        }
        return !memorySelector.selectWithIds(
                memory, phrase, "process-memory-candidate", 2_000).itemIds().isEmpty();
    }

    private boolean acceptedAssignment(ProcessAiAssignment assignment, List<String> paths) {
        String prefix = "/assignments/" + assignment.ownerRollRef() + "/";
        return paths.stream().anyMatch(path -> path.startsWith(prefix));
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
        if (assignment.sawIntent() != null) {
            return "CUTS".equals(assignment.sawIntent().type())
                    ? "SAW_CUTS" : "SAW_" + assignment.sawIntent().type();
        }
        if (normalized.contains("diameter") && assignment.rewindIntent().diameterRule() != null) {
            return assignment.rewindIntent().diameterRule().type();
        }
        if (normalized.contains("core")) return "CORE_DIAMETER";
        if (normalized.contains("width") && assignment.rewindIntent().widthRule() != null) {
            return "WIDTH_" + assignment.rewindIntent().widthRule().type();
        }
        return assignment.rewindIntent().modeIntent();
    }
}
