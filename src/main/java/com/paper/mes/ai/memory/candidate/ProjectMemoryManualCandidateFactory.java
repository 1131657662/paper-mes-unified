package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class ProjectMemoryManualCandidateFactory {

    private final ObjectMapper objectMapper;

    Optional<ProjectMemoryCandidateProposal> create(
            ProjectMemorySubmissionLearningSnapshot snapshot,
            ProjectMemorySnapshot memory) {
        return createFinalConfiguration(snapshot.finalConfiguration(), memory);
    }

    Optional<ProjectMemoryCandidateProposal> createFinalConfiguration(
            com.fasterxml.jackson.databind.JsonNode finalConfiguration,
            ProjectMemorySnapshot memory) {
        String input = safeConfigurationLabel(finalConfiguration);
        if (input == null || alreadyApproved(memory, input)) return Optional.empty();
        ObjectNode document = objectMapper.createObjectNode();
        document.put("type", "EXAMPLE");
        document.put("scope", "PROCESS_ORDER");
        document.put("status", "ACTIVE");
        document.put("input", input);
        ObjectNode expected = document.putObject("expected");
        expected.put("processType", "PROCESS_ORDER");
        expected.put("intent", "FINAL_VALIDATED_CONFIGURATION");
        expected.put("field", "processPlans");
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
        return normalized.length() < 2 || normalized.length() > 500 ? null : normalized;
    }

    private String safeConfigurationLabel(com.fasterxml.jackson.databind.JsonNode configuration) {
        var plans = configuration.path("processPlans");
        if (!plans.isArray() || plans.isEmpty()) return null;
        String modes = java.util.stream.StreamSupport.stream(plans.spliterator(), false)
                .map(plan -> plan.path("processMode").asText() + "/"
                        + plan.path("mainStepType").asText())
                .distinct().sorted().collect(Collectors.joining(","));
        return normalize("已确认工艺配置:" + modes);
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
