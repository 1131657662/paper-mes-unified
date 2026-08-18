package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.JsonNode;

public record ProjectMemorySubmissionLearningSnapshot(
        String orderUuid,
        String projectMemoryVersion,
        String customerRequirement,
        JsonNode rollContext,
        JsonNode finalConfiguration,
        String createdBy) {
}
