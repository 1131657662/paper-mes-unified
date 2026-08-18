package com.paper.mes.ai.process.session.dto;

public record ReserveProcessAiParseCommand(
        String orderUuid,
        String conversationId,
        int expectedVersion) {
}
