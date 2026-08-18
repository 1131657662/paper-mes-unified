package com.paper.mes.ai.process.session.crypto;

public record AiMessageCryptoContext(
        String conversationId,
        int sequenceNo,
        String role) {
}
