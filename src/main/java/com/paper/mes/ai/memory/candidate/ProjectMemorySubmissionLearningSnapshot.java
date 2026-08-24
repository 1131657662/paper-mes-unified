package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;

public record ProjectMemorySubmissionLearningSnapshot(
        String orderUuid,
        String projectMemoryVersion,
        String customerRequirementHash,
        JsonNode rollContext,
        JsonNode finalConfiguration,
        String createdBy) {

    public ProjectMemorySubmissionLearningSnapshot {
        customerRequirementHash = customerRequirementHash != null
                && customerRequirementHash.matches("[0-9a-fA-F]{64}")
                ? customerRequirementHash.toLowerCase(java.util.Locale.ROOT)
                : ProcessAiAuditHasher.sha256(customerRequirementHash);
    }
}
