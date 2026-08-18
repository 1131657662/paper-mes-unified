package com.paper.mes.ai.process.audit;

import lombok.Builder;

import java.util.List;

@Builder
public record ProcessAiCallAuditEntry(
        String orderUuid,
        String conversationId,
        String parseId,
        Integer expectedVersion,
        String action,
        String idempotencyKey,
        String schemaVersion,
        String projectMemoryVersion,
        String projectMemoryChecksum,
        List<String> projectMemoryItemIds,
        String requestHash,
        String resultHash,
        String provider,
        String model,
        String route,
        String outcome,
        String failureCode,
        Integer latencyMs,
        Integer inputTokens,
        Integer outputTokens,
        String createdBy) {

    public ProcessAiCallAuditEntry {
        projectMemoryItemIds = projectMemoryItemIds == null
                ? List.of() : List.copyOf(projectMemoryItemIds);
    }
}
