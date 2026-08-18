package com.paper.mes.ai.process.credential;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiProviderCredentialResolverTest {

    private final AiProviderSecretRepository repository = mock(AiProviderSecretRepository.class);
    private final AiProviderSecretCipher cipher = mock(AiProviderSecretCipher.class);
    private final AiProperties properties = new AiProperties();
    private final AiProviderCredentialResolver resolver =
            new AiProviderCredentialResolver(repository, cipher, properties);

    @Test
    void resolveApiKeyUsesAnEnabledDatabaseCredentialBeforeTheEnvironment() {
        properties.setDeepseekApiKey("environment-secret");
        when(repository.find("DEEPSEEK")).thenReturn(Optional.of(row(true)));
        when(cipher.decrypt(AiProvider.DEEPSEEK, "ciphertext")).thenReturn("database-secret");

        assertThat(resolver.resolveApiKey(AiProvider.DEEPSEEK))
                .contains("database-secret");
    }

    @Test
    void resolveApiKeyUsesTheEnvironmentWhenNoDatabaseOverrideExists() {
        properties.setDeepseekApiKey("environment-secret");
        when(repository.find("DEEPSEEK")).thenReturn(Optional.empty());

        assertThat(resolver.resolveApiKey(AiProvider.DEEPSEEK))
                .contains("environment-secret");
        assertThat(resolver.status(AiProvider.DEEPSEEK).source()).isEqualTo("ENVIRONMENT");
    }

    @Test
    void aDisabledDatabaseCredentialBlocksEnvironmentFallback() {
        properties.setDeepseekApiKey("environment-secret");
        when(repository.find("DEEPSEEK")).thenReturn(Optional.of(row(false)));

        assertThat(resolver.resolveApiKey(AiProvider.DEEPSEEK)).isEmpty();
    }

    private AiProviderSecretRow row(boolean enabled) {
        return new AiProviderSecretRow(
                "DEEPSEEK", "ciphertext", "1234", enabled,
                "user-1", LocalDateTime.parse("2026-08-16T12:00:00"));
    }
}
