package com.paper.mes.ai.process.parse;

import java.time.LocalDateTime;
import java.util.List;

record ProcessAiConfirmedRequirement(
        String schemaVersion,
        String conversationId,
        String parseId,
        int parseRevision,
        List<String> acceptedFieldPaths,
        String projectMemoryVersion,
        String projectMemoryChecksum,
        String planHash,
        String confirmedBy,
        LocalDateTime confirmedAt) {

    ProcessAiConfirmedRequirement {
        acceptedFieldPaths = List.copyOf(acceptedFieldPaths);
    }
}
