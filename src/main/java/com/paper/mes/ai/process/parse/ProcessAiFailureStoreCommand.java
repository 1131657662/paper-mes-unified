package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.memory.ProjectMemorySnapshot;

public record ProcessAiFailureStoreCommand(
        String orderUuid,
        String conversationId,
        String parseId,
        int expectedVersion,
        int parseRevision,
        int memoryGeneration,
        String requestIdempotencyKey,
        ProjectMemorySnapshot memory,
        String provider,
        String model,
        String route,
        String failureCode,
        String failureTraceId) {
}
