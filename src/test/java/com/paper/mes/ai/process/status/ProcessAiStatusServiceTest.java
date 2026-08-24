package com.paper.mes.ai.process.status;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import com.paper.mes.ai.memory.ProjectMemoryDocumentProvider;
import com.paper.mes.ai.process.credential.AiProviderCredentialResolver;
import com.paper.mes.ai.process.session.crypto.AiMessageCipher;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessAiStatusServiceTest {

    @Test
    void statusReportsReadyOnlyWhenEveryFailClosedDependencyIsAvailable() {
        AiProperties properties = properties();
        ProjectMemoryDocumentProvider memory = mock(ProjectMemoryDocumentProvider.class);
        AiMessageCipher cipher = mock(AiMessageCipher.class);
        AiProviderCredentialResolver credentials = mock(AiProviderCredentialResolver.class);
        when(memory.ready()).thenReturn(true);
        when(memory.state()).thenReturn("READY");
        when(cipher.configured()).thenReturn(true);
        when(credentials.resolveApiKey(AiProvider.DEEPSEEK)).thenReturn(Optional.of("secret"));
        when(credentials.resolveApiKey(AiProvider.ZHIPU)).thenReturn(Optional.of("fallback"));

        ProcessAiStatusResponse status = new ProcessAiStatusService(
                properties, memory, cipher, credentials).status();

        assertThat(status.ready()).isTrue();
        assertThat(status.unavailableReason()).isNull();
        assertThat(status.model()).isEqualTo("deepseek-v4-pro");
        assertThat(status.fallbackModel()).isEqualTo("glm-4.7-flash");
        assertThat(status.fallbackConfigured()).isTrue();
    }

    @Test
    void statusExplainsWhenTheProviderKeyIsMissing() {
        AiProperties properties = properties();
        ProjectMemoryDocumentProvider memory = mock(ProjectMemoryDocumentProvider.class);
        AiMessageCipher cipher = mock(AiMessageCipher.class);
        AiProviderCredentialResolver credentials = mock(AiProviderCredentialResolver.class);
        when(memory.ready()).thenReturn(true);
        when(memory.state()).thenReturn("READY");
        when(cipher.configured()).thenReturn(true);
        when(credentials.resolveApiKey(AiProvider.DEEPSEEK)).thenReturn(Optional.empty());
        when(credentials.resolveApiKey(AiProvider.ZHIPU)).thenReturn(Optional.empty());

        ProcessAiStatusResponse status = new ProcessAiStatusService(
                properties, memory, cipher, credentials).status();

        assertThat(status.ready()).isFalse();
        assertThat(status.unavailableReason()).isEqualTo("AI_PROVIDER_NOT_CONFIGURED");
    }

    @Test
    void statusKeepsPrimaryConfigurationFalseWhenOnlyGlmFallbackIsAvailable() {
        AiProperties properties = properties();
        ProjectMemoryDocumentProvider memory = mock(ProjectMemoryDocumentProvider.class);
        AiMessageCipher cipher = mock(AiMessageCipher.class);
        AiProviderCredentialResolver credentials = mock(AiProviderCredentialResolver.class);
        when(memory.ready()).thenReturn(true);
        when(memory.state()).thenReturn("READY");
        when(cipher.configured()).thenReturn(true);
        when(credentials.resolveApiKey(AiProvider.DEEPSEEK)).thenReturn(Optional.empty());
        when(credentials.resolveApiKey(AiProvider.ZHIPU)).thenReturn(Optional.of("fallback"));

        ProcessAiStatusResponse status = new ProcessAiStatusService(
                properties, memory, cipher, credentials).status();

        assertThat(status.ready()).isTrue();
        assertThat(status.providerConfigured()).isFalse();
        assertThat(status.fallbackConfigured()).isTrue();
    }

    @Test
    void statusFailsClosedWhenMemoryReferenceHmacKeyIsMissing() {
        AiProperties properties = properties();
        properties.setMemoryReferenceHmacKey("");
        ProjectMemoryDocumentProvider memory = mock(ProjectMemoryDocumentProvider.class);
        AiMessageCipher cipher = mock(AiMessageCipher.class);
        AiProviderCredentialResolver credentials = mock(AiProviderCredentialResolver.class);
        when(memory.ready()).thenReturn(true);
        when(memory.state()).thenReturn("READY");
        when(cipher.configured()).thenReturn(true);
        when(credentials.resolveApiKey(AiProvider.DEEPSEEK)).thenReturn(Optional.of("secret"));
        when(credentials.resolveApiKey(AiProvider.ZHIPU)).thenReturn(Optional.empty());

        ProcessAiStatusResponse status = new ProcessAiStatusService(
                properties, memory, cipher, credentials).status();

        assertThat(status.ready()).isFalse();
        assertThat(status.unavailableReason()).isEqualTo("AI_MEMORY_REFERENCE_HMAC_UNAVAILABLE");
    }

    private AiProperties properties() {
        AiProperties properties = new AiProperties();
        properties.setDataMode("CONTEXT_ALLOWLIST");
        properties.setProvider("DEEPSEEK");
        properties.setDeepseekApiKey("secret");
        properties.setMemoryReferenceHmacKey("h".repeat(32));
        return properties;
    }
}
