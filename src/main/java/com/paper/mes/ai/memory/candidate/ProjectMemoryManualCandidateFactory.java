package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ProjectMemoryManualCandidateFactory {

    private final ObjectMapper objectMapper;

    Optional<ProjectMemoryCandidateProposal> create(
            ProjectMemorySubmissionLearningSnapshot snapshot,
            ProjectMemorySnapshot memory) {
        String input = normalize(snapshot.customerRequirement());
        if (input == null || alreadyApproved(memory, input)) return Optional.empty();
        ObjectNode document = objectMapper.createObjectNode();
        document.put("type", "EXAMPLE");
        document.put("scope", "PROCESS_ORDER");
        document.put("status", "ACTIVE");
        document.put("input", input);
        document.set("expected", snapshot.finalConfiguration());
        document.put("evidenceRequired", true);
        document.put("source", "manual-final-configuration-after-ai-conversation");
        String hash = ProcessAiAuditHasher.sha256(input.toLowerCase(Locale.ROOT));
        return Optional.of(new ProjectMemoryCandidateProposal(
                "example-candidate-" + hash.substring(0, 24), "EXAMPLE",
                "PROCESS_ORDER", "FINAL_VALIDATED_CONFIGURATION",
                input, document, null));
    }

    private String normalize(String value) {
        if (value == null) return null;
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() < 2 || normalized.length() > 2_000 ? null : normalized;
    }

    private boolean alreadyApproved(ProjectMemorySnapshot memory, String input) {
        var examples = memory.document().path("examples");
        if (!examples.isObject()) return false;
        var fields = examples.fields();
        while (fields.hasNext()) {
            var value = fields.next().getValue();
            if ("ACTIVE".equals(value.path("status").asText())
                    && input.equals(value.path("input").asText())) return true;
        }
        return false;
    }
}
