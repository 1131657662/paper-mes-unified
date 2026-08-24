package com.paper.mes.ai.process.parse;

import java.time.LocalDateTime;

public record ProcessAiParseRecord(
        String uuid,
        String orderUuid,
        String conversationId,
        String parseId,
        int parseRevision,
        int memoryGeneration,
        String requestIdempotencyKey,
        int expectedVersion,
        String status,
        String provider,
        String model,
        String route,
        String schemaVersion,
        String projectMemoryVersion,
        String projectMemoryChecksum,
        String projectMemoryItemIds,
        String intentJson,
        String resultHash,
        ProcessAiParseConfirmation confirmation,
        LocalDateTime createdAt,
        String dialogueState,
        String resultKind,
        int workflowVersion,
        String understandingJson,
        String questionJson,
        String correctionsJson,
        String inputHash,
        String contextHash,
        String previewHash,
        String failureCode,
        String failureTraceId,
        String requiredDefaultIds,
        String acknowledgedDefaultIds) {

    /** Compatibility constructor for schema-v1 fixtures and old replay rows. */
    public ProcessAiParseRecord(
            String uuid, String orderUuid, String conversationId, String parseId,
            int parseRevision, int memoryGeneration, String requestIdempotencyKey,
            int expectedVersion, String status, String provider, String model, String route,
            String schemaVersion, String projectMemoryVersion, String projectMemoryChecksum,
            String projectMemoryItemIds, String intentJson, String resultHash,
            ProcessAiParseConfirmation confirmation, LocalDateTime createdAt) {
        this(uuid, orderUuid, conversationId, parseId, parseRevision, memoryGeneration,
                requestIdempotencyKey, expectedVersion, status, provider, model, route,
                schemaVersion, projectMemoryVersion, projectMemoryChecksum, projectMemoryItemIds,
                intentJson, resultHash, confirmation, createdAt, legacyDialogueState(status),
                "EXTRACTION", 1, null, null, null, null, null, null, null, null, null, null);
    }

    private static String legacyDialogueState(String status) {
        return switch (status) {
            case "CLARIFICATION" -> "CLARIFYING";
            case "CONFIRMED" -> "COMPLETED";
            case "REJECTED", "EXPIRED" -> "COMPLETED";
            case "INTERRUPTED" -> "FAILED";
            default -> "PREVIEW_READY";
        };
    }
}
