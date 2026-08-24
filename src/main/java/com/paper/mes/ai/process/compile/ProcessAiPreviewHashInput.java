package com.paper.mes.ai.process.compile;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record ProcessAiPreviewHashInput(
        String orderUuid,
        int expectedVersion,
        String conversationId,
        int memoryGeneration,
        String projectMemoryVersion,
        String projectMemoryChecksum,
        String resultHash,
        String finalExtractionHash,
        String correctionsHash,
        List<String> effectiveDefaults,
        JsonNode rollConfigurations,
        JsonNode compiledPlans,
        JsonNode packagingCandidates) {

    public ProcessAiPreviewHashInput {
        effectiveDefaults = effectiveDefaults == null ? List.of() : List.copyOf(effectiveDefaults);
    }

    /** Compatibility constructor for previews created before Step 3 AI configuration was explicit. */
    public ProcessAiPreviewHashInput(
            String orderUuid, int expectedVersion, String conversationId, int memoryGeneration,
            String projectMemoryVersion, String projectMemoryChecksum, String resultHash,
            String finalExtractionHash, String correctionsHash, List<String> effectiveDefaults,
            JsonNode compiledPlans, JsonNode packagingCandidates) {
        this(orderUuid, expectedVersion, conversationId, memoryGeneration, projectMemoryVersion,
                projectMemoryChecksum, resultHash, finalExtractionHash, correctionsHash,
                effectiveDefaults, null, compiledPlans, packagingCandidates);
    }
}
