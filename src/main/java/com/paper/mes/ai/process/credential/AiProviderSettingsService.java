package com.paper.mes.ai.process.credential;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import com.paper.mes.ai.process.credential.dto.AiProviderKeyUpdateRequest;
import com.paper.mes.ai.process.credential.dto.AiProviderSettingsResponse;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiProviderSettingsService {

    private static final AiProvider PROVIDER = AiProvider.DEEPSEEK;

    private final AiProviderSecretRepository repository;
    private final AiProviderSecretCipher cipher;
    private final AiProviderCredentialResolver resolver;
    private final PermissionChecker permissionChecker;
    private final AiProperties properties;

    @Transactional(readOnly = true)
    public AiProviderSettingsResponse get() {
        permissionChecker.require(Permissions.SYSTEM_CONFIG);
        return response(resolver.status(PROVIDER));
    }

    @Transactional
    public AiProviderSettingsResponse update(AiProviderKeyUpdateRequest request) {
        permissionChecker.require(Permissions.SYSTEM_CONFIG);
        String apiKey = request.getApiKey().trim();
        repository.upsert(new AiProviderSecretRow(
                PROVIDER.name(), cipher.encrypt(PROVIDER, apiKey), lastFour(apiKey),
                true, currentUserUuid(), null));
        return response(resolver.status(PROVIDER));
    }

    @Transactional
    public AiProviderSettingsResponse delete() {
        permissionChecker.require(Permissions.SYSTEM_CONFIG);
        repository.delete(PROVIDER.name());
        return response(resolver.status(PROVIDER));
    }

    private AiProviderSettingsResponse response(AiProviderCredentialStatus status) {
        return new AiProviderSettingsResponse(
                status.provider(), properties.getDeepseekModelPro(),
                properties.getDeepseekBaseUrl(), status.configured(), status.source(),
                status.maskedApiKey(), status.enabled(), status.databaseStorageReady(),
                status.updatedBy(), status.updatedAt());
    }

    private String lastFour(String value) {
        return value.substring(Math.max(0, value.length() - 4));
    }

    private String currentUserUuid() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        return user == null || user.getUuid() == null ? "system" : user.getUuid();
    }
}
