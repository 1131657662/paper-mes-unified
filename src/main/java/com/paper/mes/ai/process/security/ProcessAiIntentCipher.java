package com.paper.mes.ai.process.security;

import com.paper.mes.ai.process.session.crypto.AiMessageCryptoContext;
import com.paper.mes.ai.process.session.crypto.AiStructuredResultCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessAiIntentCipher {

    private final AiStructuredResultCipher cipher;

    public String encrypt(String conversationId, int parseRevision, String plaintext) {
        return cipher.encrypt(context(conversationId, parseRevision), plaintext);
    }

    public String decrypt(String conversationId, int parseRevision, String storedValue) {
        return cipher.decrypt(context(conversationId, parseRevision), storedValue);
    }

    private AiMessageCryptoContext context(String conversationId, int parseRevision) {
        return new AiMessageCryptoContext(conversationId, parseRevision, "PARSE_INTENT");
    }
}
