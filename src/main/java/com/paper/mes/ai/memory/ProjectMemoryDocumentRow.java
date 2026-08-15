package com.paper.mes.ai.memory;

/** Database representation used by the memory repository. */
public record ProjectMemoryDocumentRow(
        String uuid,
        String docVersion,
        String schemaVersion,
        String checksum,
        String docJson,
        String status,
        String patchNotes,
        String createdBy,
        String approvedBy) {
}
