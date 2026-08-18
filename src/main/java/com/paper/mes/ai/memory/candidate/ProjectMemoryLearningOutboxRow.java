package com.paper.mes.ai.memory.candidate;

import java.time.LocalDateTime;

record ProjectMemoryLearningOutboxRow(
        String uuid,
        String eventKey,
        String eventType,
        String payloadJson,
        int attemptCount,
        LocalDateTime nextAttemptAt) {
}
