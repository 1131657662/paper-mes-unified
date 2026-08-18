package com.paper.mes.ai.process.session.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class AiStructuredResultCipherTest {

    private AiStructuredResultCipher structuredCipher;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        properties.setMessageEncryptionKey(Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        structuredCipher = new AiStructuredResultCipher(
                new AiMessageCipher(properties), new ObjectMapper());
    }

    @Test
    void encryptedEnvelopeRoundTripsWithoutExposingPlaintext() {
        var context = new AiMessageCryptoContext("conversation-1", 2, "ASSISTANT");

        String stored = structuredCipher.encrypt(context, "{\"unitPrice\":20}");

        assertThat(stored).contains("aes-gcm-v1").doesNotContain("unitPrice", "{\"unitPrice\":20}");
        assertThat(structuredCipher.decrypt(context, stored)).isEqualTo("{\"unitPrice\":20}");
    }

    @Test
    void encryptedEnvelopeCannotMoveBetweenStructuredResultPurposes() {
        var assistant = new AiMessageCryptoContext("conversation-1", 2, "ASSISTANT");
        var confirmation = new AiMessageCryptoContext("conversation-1", 2, "CONFIRMATION");
        String stored = structuredCipher.encrypt(assistant, "{\"status\":\"READY\"}");

        BusinessException error = catchThrowableOfType(
                () -> structuredCipher.decrypt(confirmation, stored), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_MESSAGE_DECRYPT_FAILED");
    }

    @Test
    void legacyPlaintextJsonRemainsReadable() {
        var context = new AiMessageCryptoContext("conversation-1", 2, "ASSISTANT");

        assertThat(structuredCipher.decrypt(context, "{\"status\":\"READY\"}"))
                .isEqualTo("{\"status\":\"READY\"}");
    }
}
