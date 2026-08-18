package com.paper.mes.ai.process.session.dto;

public record UpdateAssistantMessageCommand(
        String orderUuid,
        String conversationId,
        int expectedVersion,
        int sequenceNo,
        String content,
        String status,
        String structuredResult) {
}
