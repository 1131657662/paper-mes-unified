package com.paper.mes.ai.process.session;

import java.time.LocalDateTime;

record ProcessAiMessageRow(
        String uuid,
        String conversationId,
        int memoryGeneration,
        int sequenceNo,
        String role,
        String messageStatus,
        String idempotencyKey,
        String contentCiphertext,
        String contentHash,
        String structuredResult,
        LocalDateTime createdAt) {
}
