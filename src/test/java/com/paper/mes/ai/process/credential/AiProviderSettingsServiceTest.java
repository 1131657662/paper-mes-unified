package com.paper.mes.ai.process.credential;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import com.paper.mes.ai.process.credential.dto.AiProviderKeyUpdateRequest;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.RoleCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiProviderSettingsServiceTest {

    private final AiProviderSecretRepository repository = mock(AiProviderSecretRepository.class);
    private final AiProviderSecretCipher cipher = mock(AiProviderSecretCipher.class);
    private final AiProviderCredentialResolver resolver = mock(AiProviderCredentialResolver.class);
    private AiProviderSettingsService service;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        service = new AiProviderSettingsService(
                repository, cipher, resolver, new PermissionChecker(), properties);
        AuthContextHolder.setCurrentUser(CurrentUser.builder()
                .uuid("admin-1").roleCode(RoleCodes.ADMIN).build());
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void updateStoresOnlyCiphertextAndLastFourCharacters() {
        AiProviderKeyUpdateRequest request = new AiProviderKeyUpdateRequest();
        request.setApiKey("sk-sensitive-1234");
        when(cipher.encrypt(AiProvider.DEEPSEEK, "sk-sensitive-1234"))
                .thenReturn("v1:ciphertext");
        when(resolver.status(AiProvider.DEEPSEEK)).thenReturn(status());

        service.update("deepseek", request);

        ArgumentCaptor<AiProviderSecretRow> captor =
                ArgumentCaptor.forClass(AiProviderSecretRow.class);
        verify(repository).upsert(captor.capture());
        assertThat(captor.getValue().ciphertext()).isEqualTo("v1:ciphertext");
        assertThat(captor.getValue().lastFour()).isEqualTo("1234");
        assertThat(captor.getValue().updatedBy()).isEqualTo("admin-1");
        assertThat(request.toString()).doesNotContain("sk-sensitive-1234");
    }

    @Test
    void updateStoresZhipuCredentialUnderItsOwnProvider() {
        AiProviderKeyUpdateRequest request = new AiProviderKeyUpdateRequest();
        request.setApiKey("glm-sensitive-5678");
        when(cipher.encrypt(AiProvider.ZHIPU, "glm-sensitive-5678"))
                .thenReturn("v1:glm-ciphertext");
        when(resolver.status(AiProvider.ZHIPU)).thenReturn(status(AiProvider.ZHIPU));

        service.update("ZHIPU", request);

        ArgumentCaptor<AiProviderSecretRow> captor =
                ArgumentCaptor.forClass(AiProviderSecretRow.class);
        verify(repository).upsert(captor.capture());
        assertThat(captor.getValue().provider()).isEqualTo("ZHIPU");
        assertThat(captor.getValue().ciphertext()).isEqualTo("v1:glm-ciphertext");
        assertThat(captor.getValue().lastFour()).isEqualTo("5678");
        assertThat(request.toString()).doesNotContain("glm-sensitive-5678");
    }

    @Test
    void rejectsProvidersThatAreNotManagedCredentials() {
        AiProviderKeyUpdateRequest request = new AiProviderKeyUpdateRequest();
        request.setApiKey("sk-sensitive-1234");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.update("local_rules", request))
                .isInstanceOf(IllegalArgumentException.class);
        org.mockito.Mockito.verifyNoInteractions(cipher, repository, resolver);
    }

    private AiProviderCredentialStatus status() {
        return status(AiProvider.DEEPSEEK);
    }

    private AiProviderCredentialStatus status(AiProvider provider) {
        return new AiProviderCredentialStatus(
                provider.name(), true, "DATABASE", "****1234",
                true, true, "admin-1", null);
    }
}
