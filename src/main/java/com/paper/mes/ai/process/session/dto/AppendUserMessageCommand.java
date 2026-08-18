package com.paper.mes.ai.process.session.dto;

public record AppendUserMessageCommand(
        String orderUuid,
        String conversationId,
        int expectedVersion,
        String idempotencyKey,
        String content) {
}
