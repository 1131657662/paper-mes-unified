package com.paper.mes.ai.process.parse;

import java.time.LocalDateTime;

record ProcessAiPackagingCandidateRow(
        String uuid,
        String orderUuid,
        String conversationId,
        String parseId,
        int parseRevision,
        String ownerRollRef,
        String originalUuid,
        String status,
        String createdBy,
        String confirmedResultJson,
        LocalDateTime createdAt) {
}
