package com.paper.mes.ai.memory.candidate;

import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiAncillaryRequirements;
import com.paper.mes.ai.process.intent.ProcessAiEvidence;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiLabelRequirement;
import com.paper.mes.ai.process.intent.ProcessAiPackagingRequirement;

import java.util.List;

/** Minimal confirmed-learning payload; it deliberately contains no order or parse identifiers. */
public record ProjectMemoryCandidateLearningSnapshot(
        String orderRefHash,
        String parseRefHash,
        String projectMemoryVersion,
        String customerRequirementHash,
        String confirmedBy,
        ProcessAiExtractionResult extraction,
        List<String> acceptedFieldPaths,
        int planCount,
        boolean previewReady) {

    public ProjectMemoryCandidateLearningSnapshot {
        acceptedFieldPaths = acceptedFieldPaths == null ? List.of() : List.copyOf(acceptedFieldPaths);
    }

    static ProjectMemoryCandidateLearningSnapshot from(ProjectMemoryCandidateConfirmedEvent event,
                                                       String orderRefHash, String parseRefHash) {
        ProcessAiExtractionResult extraction = event.extraction() == null ? null
                : withoutEvidence(event.extraction());
        int plans = event.compilation() == null ? 0 : event.compilation().plans().size();
        boolean ready = event.compilation() != null && event.compilation().eligible();
        return new ProjectMemoryCandidateLearningSnapshot(orderRefHash, parseRefHash,
                event.projectMemoryVersion(), event.customerRequirementHash(), event.confirmedBy(),
                extraction, event.acceptedFieldPaths(), plans, ready);
    }

    private static ProcessAiExtractionResult withoutEvidence(ProcessAiExtractionResult value) {
        List<ProcessAiAssignment> assignments = value.assignments().stream()
                .map(assignment -> new ProcessAiAssignment(assignment.sourceRollRefs(),
                        assignment.ownerRollRef(), assignment.coveredRollRefs(), assignment.processType(),
                        assignment.processMode(), assignment.rewindIntent(), assignment.sawIntent(), assignment.ancillaryRequirements(),
                        List.<ProcessAiEvidence>of(), List.of()))
                .toList();
        assignments = assignments.stream().map(ProjectMemoryCandidateLearningSnapshot::withoutAncillaryText)
                .toList();
        return new ProcessAiExtractionResult("redacted", value.schemaVersion(), assignments,
                List.of(), List.of(), false, List.of());
    }

    private static ProcessAiAssignment withoutAncillaryText(ProcessAiAssignment assignment) {
        ProcessAiAncillaryRequirements value = assignment.ancillaryRequirements();
        if (value == null) return assignment;
        ProcessAiLabelRequirement label = value.label() == null ? null
                : new ProcessAiLabelRequirement(value.label().required(), null, value.label().createsServiceStep());
        ProcessAiPackagingRequirement packaging = value.packaging() == null ? null
                : new ProcessAiPackagingRequirement(value.packaging().type(), "[金额]",
                value.packaging().unit(), value.packaging().quantityMode(), value.packaging().createsServiceStep());
        return new ProcessAiAssignment(assignment.sourceRollRefs(), assignment.ownerRollRef(),
                assignment.coveredRollRefs(), assignment.processType(), assignment.processMode(), assignment.rewindIntent(),
                assignment.sawIntent(), new ProcessAiAncillaryRequirements(label, packaging),
                assignment.evidence(), List.of());
    }
}
