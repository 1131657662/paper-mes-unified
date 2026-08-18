package com.paper.mes.ai.process.session.dto;

public record ProcessAiParseReservation(
        String conversationId,
        int parseRevision,
        String projectMemoryVersion,
        int memoryGeneration) {
}
