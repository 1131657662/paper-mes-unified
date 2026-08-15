package com.paper.mes.ai.memory;

/** Audit and idempotency record for a project-memory mutation. */
public record ProjectMemoryPatchAuditRow(
        String uuid,
        String idempotencyKey,
        String operationType,
        String expectedMemoryVersion,
        String oldDocVersion,
        String newDocVersion,
        String oldChecksum,
        String newChecksum,
        String operationsJson,
        String reason,
        String operator) {
}
