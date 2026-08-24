package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.JsonNode;

/** Outbox-only form of a submitted-order learning event without reversible identifiers. */
public record ProjectMemorySubmissionLearningOutboxSnapshot(
        String orderRefHash,
        String projectMemoryVersion,
        String customerRequirementHash,
        JsonNode rollContext,
        JsonNode finalConfiguration,
        String createdBy) {
}
