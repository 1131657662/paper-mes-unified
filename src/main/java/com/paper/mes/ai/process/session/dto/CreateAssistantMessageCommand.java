package com.paper.mes.ai.process.session.dto;

public record CreateAssistantMessageCommand(
        String orderUuid,
        String conversationId,
        int expectedVersion) {
}
