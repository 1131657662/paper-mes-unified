package com.paper.mes.ai.memory.candidate;

import java.time.LocalDateTime;

record ProjectMemoryCandidateRow(
        String uuid,
        String memoryId,
        String candidateType,
        String candidateJson,
        String status,
        int distinctOrderCount,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt,
        LocalDateTime expiresAt,
        String reviewedBy,
        String reviewNotes,
        LocalDateTime reviewedAt) {
}
