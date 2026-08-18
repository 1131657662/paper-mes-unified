package com.paper.mes.ai.process.credential;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import com.paper.mes.ai.process.session.crypto.AiMessageCipher;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiProviderSecretCipherTest {

    @Test
    void encryptRoundTripsWithProviderBoundAuthenticatedEncryption() {
        AiProviderSecretCipher cipher = new AiProviderSecretCipher(configuredProperties());

        String ciphertext = cipher.encrypt(AiProvider.DEEPSEEK, "sk-secret-value");

        assertThat(ciphertext).startsWith("v1:").doesNotContain("sk-secret-value");
        assertThat(cipher.decrypt(AiProvider.DEEPSEEK, ciphertext))
                .isEqualTo("sk-secret-value");
        assertThatThrownBy(() -> cipher.decrypt(AiProvider.ZHIPU, ciphertext))
                .hasMessageContaining("credential encryption");
    }

    @Test
    void configuredIsFalseWithoutASeparateMasterKey() {
        assertThat(new AiProviderSecretCipher(new AiProperties()).configured()).isFalse();
    }

    @Test
    void springCanWireBothAiEncryptionComponents() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AiProperties.class);
            context.register(AiProviderSecretCipher.class, AiMessageCipher.class);
            context.refresh();

            assertThat(context.getBean(AiProviderSecretCipher.class)).isNotNull();
            assertThat(context.getBean(AiMessageCipher.class)).isNotNull();
        }
    }

    private AiProperties configuredProperties() {
        AiProperties properties = new AiProperties();
        properties.setConfigMasterKey(Base64.getEncoder().encodeToString(new byte[32]));
        return properties;
    }
}
