package com.paper.mes.ai.memory.candidate;

import java.time.LocalDateTime;

record ProjectMemoryCandidateEvidenceRow(
        String uuid,
        String candidateUuid,
        String sourceType,
        String proposedValueJson,
        String finalValueJson,
        String differenceJson,
        Boolean previewReady,
        String createdBy,
        LocalDateTime createdAt) {
}
