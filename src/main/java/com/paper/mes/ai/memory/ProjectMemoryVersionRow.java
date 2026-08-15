package com.paper.mes.ai.memory;

import java.time.LocalDateTime;

/** Read-only metadata for one retained project-memory snapshot. */
public record ProjectMemoryVersionRow(
        String docVersion,
        String schemaVersion,
        String checksum,
        String status,
        String patchNotes,
        String createdBy,
        String approvedBy,
        LocalDateTime createdAt) {
}
