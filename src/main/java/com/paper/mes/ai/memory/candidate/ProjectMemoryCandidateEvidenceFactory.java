package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class ProjectMemoryCandidateEvidenceFactory {

    private final ObjectMapper objectMapper;

    ProjectMemoryCandidateEvidenceWrite confirmed(
            ProjectMemoryCandidateConfirmedEvent event,
            ProjectMemoryCandidateProposal proposal) {
        ObjectNode context = objectMapper.createObjectNode();
        context.put("customerRequirement", event.customerRequirement());
        context.set("baseline", objectMapper.valueToTree(event.baseline()));
        List<ProcessAiCompiledPlan> plans = event.compilation().plans().stream()
                .filter(plan -> plan.ownerRollRef().equals(proposal.assignment().ownerRollRef()))
                .toList();
        ObjectNode difference = objectMapper.createObjectNode();
        difference.set("acceptedFieldPaths", objectMapper.valueToTree(
                event.acceptedFieldPaths().stream().filter(path -> path.startsWith(
                        "/assignments/" + proposal.assignment().ownerRollRef() + "/")).toList()));
        boolean ready = !plans.isEmpty() && plans.stream().allMatch(
                plan -> plan.preview() != null && plan.preview().isReady());
        return new ProjectMemoryCandidateEvidenceWrite(
                event.orderUuid(), event.parseId(), "AI_CONFIRMED", proposal.phrase(),
                json(context), json(proposal.assignment()), json(plans), json(difference),
                ready, event.confirmedBy(), evidenceHash(proposal, json(plans)));
    }

    ProjectMemoryCandidateEvidenceWrite manual(
            ProjectMemorySubmissionLearningSnapshot snapshot,
            ProjectMemoryCandidateProposal proposal) {
        ObjectNode difference = objectMapper.createObjectNode();
        difference.put("kind", "MANUAL_FINAL_CONFIGURATION_AFTER_AI_CONVERSATION");
        return new ProjectMemoryCandidateEvidenceWrite(
                snapshot.orderUuid(), null, "MANUAL_FINAL", proposal.phrase(),
                json(snapshot.rollContext()), null, json(snapshot.finalConfiguration()),
                json(difference), true, snapshot.createdBy(),
                evidenceHash(proposal, json(snapshot.finalConfiguration())));
    }

    private String evidenceHash(ProjectMemoryCandidateProposal proposal, String finalJson) {
        return ProcessAiAuditHasher.sha256(
                proposal.phrase(), proposal.scope(), proposal.intent(), finalJson);
    }

    private String json(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("memory candidate evidence serialization failed", exception);
        }
    }
}
