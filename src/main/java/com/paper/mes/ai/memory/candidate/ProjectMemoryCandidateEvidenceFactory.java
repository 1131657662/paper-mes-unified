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
    private final ProcessAiMemoryReferenceHasher referenceHasher;

    ProjectMemoryCandidateEvidenceWrite confirmed(
            ProjectMemoryCandidateConfirmedEvent event,
            ProjectMemoryCandidateProposal proposal) {
        ObjectNode context = objectMapper.createObjectNode();
        context.put("scope", proposal.scope());
        context.put("candidateType", proposal.candidateType());
        List<ProcessAiCompiledPlan> plans = event.compilation().plans().stream()
                .filter(plan -> plan.ownerRollRef().equals(proposal.assignment().ownerRollRef()))
                .toList();
        ObjectNode difference = objectMapper.createObjectNode();
        difference.put("acceptedFieldCount", event.acceptedFieldPaths().stream()
                .filter(path -> path.startsWith("/assignments/"))
                .count());
        boolean ready = !plans.isEmpty() && plans.stream().allMatch(
                plan -> plan.preview() != null && plan.preview().isReady());
        return new ProjectMemoryCandidateEvidenceWrite(
                null, null, "AI_CONFIRMED", null,
                null, json(sharedProposal(proposal)), json(sharedPlans(plans)), json(difference),
                ready, event.confirmedBy(), evidenceHash(proposal, json(plans)),
                referenceHasher.hash(event.orderUuid()), referenceHasher.hash(event.parseId()),
                null, ProcessAiAuditHasher.sha256(event.customerRequirementHash(), json(context)));
    }

    ProjectMemoryCandidateEvidenceWrite confirmedSnapshot(
            ProjectMemoryCandidateLearningSnapshot event,
            ProjectMemoryCandidateProposal proposal) {
        ObjectNode difference = objectMapper.createObjectNode();
        difference.put("acceptedFieldCount", event.acceptedFieldPaths().size());
        return new ProjectMemoryCandidateEvidenceWrite(
                null, null, "AI_CONFIRMED", null, null,
                json(sharedProposal(proposal)), json(sharedPlans(event.planCount(), event.previewReady())),
                json(difference), event.previewReady(), event.confirmedBy(),
                ProcessAiAuditHasher.sha256(proposal.phrase(), proposal.scope(), proposal.intent(),
                        Integer.toString(event.planCount())), event.orderRefHash(), event.parseRefHash(),
                null, ProcessAiAuditHasher.sha256(event.customerRequirementHash(), json(difference)));
    }

    ProjectMemoryCandidateEvidenceWrite manual(
            ProjectMemorySubmissionLearningSnapshot snapshot,
            ProjectMemoryCandidateProposal proposal) {
        ObjectNode difference = objectMapper.createObjectNode();
        difference.put("kind", "MANUAL_FINAL_CONFIGURATION_AFTER_AI_CONVERSATION");
        return new ProjectMemoryCandidateEvidenceWrite(
                null, null, "MANUAL_FINAL", null,
                null, null, "{}",
                json(difference), true, snapshot.createdBy(),
                evidenceHash(proposal, json(snapshot.finalConfiguration())),
                referenceHasher.hash(snapshot.orderUuid()), null, null,
                ProcessAiAuditHasher.sha256(snapshot.customerRequirementHash(), json(difference)));
    }

    ProjectMemoryCandidateEvidenceWrite manualSnapshot(
            ProjectMemorySubmissionLearningOutboxSnapshot snapshot,
            ProjectMemoryCandidateProposal proposal) {
        ObjectNode difference = objectMapper.createObjectNode();
        difference.put("kind", "MANUAL_FINAL_CONFIGURATION_AFTER_AI_CONVERSATION");
        return new ProjectMemoryCandidateEvidenceWrite(
                null, null, "MANUAL_FINAL", null, null, null, "{}",
                json(difference), true, snapshot.createdBy(),
                evidenceHash(proposal, json(snapshot.finalConfiguration())),
                snapshot.orderRefHash(), null, null,
                ProcessAiAuditHasher.sha256(snapshot.customerRequirementHash(), json(difference)));
    }

    private String evidenceHash(ProjectMemoryCandidateProposal proposal, String finalJson) {
        return ProcessAiAuditHasher.sha256(
                proposal.phrase(), proposal.scope(), proposal.intent(), finalJson);
    }

    private ObjectNode sharedProposal(ProjectMemoryCandidateProposal proposal) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("candidateType", proposal.candidateType());
        result.put("scope", proposal.scope());
        result.put("intent", proposal.intent());
        return result;
    }

    private ObjectNode sharedPlans(List<ProcessAiCompiledPlan> plans) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("previewPlanCount", plans.size());
        result.put("previewReady", !plans.isEmpty() && plans.stream()
                .allMatch(plan -> plan.preview() != null && plan.preview().isReady()));
        return result;
    }

    private ObjectNode sharedPlans(int planCount, boolean previewReady) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("previewPlanCount", planCount);
        result.put("previewReady", previewReady);
        return result;
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
