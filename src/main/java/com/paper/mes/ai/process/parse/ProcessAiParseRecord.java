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
        LocalDateTime createdAt) {
}
