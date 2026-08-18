package com.paper.mes.ai.process.session.crypto;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class AiMessageCipherTest {

    private AiProperties properties;
    private AiMessageCipher cipher;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setMessageEncryptionKey(Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        cipher = new AiMessageCipher(properties);
    }

    @Test
    void encryptRoundTripsUtf8WithoutStoringPlaintext() {
        AiMessageCryptoContext context = new AiMessageCryptoContext("conversation-1", 1, "USER");

        String encrypted = cipher.encrypt(context, "客户说切2刀");

        assertThat(encrypted).startsWith("v1:").doesNotContain("客户说切2刀");
        assertThat(cipher.decrypt(context, encrypted)).isEqualTo("客户说切2刀");
    }

    @Test
    void encryptUsesANewIvForEveryMessage() {
        AiMessageCryptoContext context = new AiMessageCryptoContext("conversation-1", 1, "USER");

        String first = cipher.encrypt(context, "相同内容");
        String second = cipher.encrypt(context, "相同内容");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void decryptRejectsCiphertextMovedToAnotherConversation() {
        var original = new AiMessageCryptoContext("conversation-1", 1, "USER");
        String encrypted = cipher.encrypt(original, "客户原话");

        BusinessException error = catchThrowableOfType(() -> cipher.decrypt(
                new AiMessageCryptoContext("conversation-2", 1, "USER"), encrypted),
                BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_MESSAGE_DECRYPT_FAILED");
    }

    @Test
    void encryptFailsClosedWhenTheConfiguredKeyIsInvalid() {
        properties.setMessageEncryptionKey("not-a-32-byte-key");

        BusinessException error = catchThrowableOfType(() -> cipher.encrypt(
                new AiMessageCryptoContext("conversation-1", 1, "USER"), "内容"),
                BusinessException.class);

        assertThat(error.getCode()).isEqualTo(503);
        assertThat(error.getErrorCode()).isEqualTo("AI_MESSAGE_KEY_UNAVAILABLE");
    }
}
