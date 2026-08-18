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

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AiProviderSettingsService {

    private final AiProviderSecretRepository repository;
    private final AiProviderSecretCipher cipher;
    private final AiProviderCredentialResolver resolver;
    private final PermissionChecker permissionChecker;
    private final AiProperties properties;

    @Transactional(readOnly = true)
    public AiProviderSettingsResponse get(String providerValue) {
        permissionChecker.require(Permissions.SYSTEM_CONFIG);
        AiProvider provider = managedProvider(providerValue);
        return response(resolver.status(provider));
    }

    @Transactional
    public AiProviderSettingsResponse update(String providerValue, AiProviderKeyUpdateRequest request) {
        permissionChecker.require(Permissions.SYSTEM_CONFIG);
        AiProvider provider = managedProvider(providerValue);
        String apiKey = request.getApiKey().trim();
        repository.upsert(new AiProviderSecretRow(
                provider.name(), cipher.encrypt(provider, apiKey), lastFour(apiKey),
                true, currentUserUuid(), null));
        return response(resolver.status(provider));
    }

    @Transactional
    public AiProviderSettingsResponse delete(String providerValue) {
        permissionChecker.require(Permissions.SYSTEM_CONFIG);
        AiProvider provider = managedProvider(providerValue);
        repository.delete(provider.name());
        return response(resolver.status(provider));
    }

    private AiProviderSettingsResponse response(AiProviderCredentialStatus status) {
        AiProvider provider = managedProvider(status.provider());
        return new AiProviderSettingsResponse(
                status.provider(), model(provider), baseUrl(provider),
                status.configured(), status.source(),
                status.maskedApiKey(), status.enabled(), status.databaseStorageReady(),
                status.updatedBy(), status.updatedAt());
    }

    private AiProvider managedProvider(String value) {
        if (value == null) throw new IllegalArgumentException("AI 供应商不能为空");
        try {
            AiProvider provider = AiProvider.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (provider == AiProvider.DEEPSEEK || provider == AiProvider.ZHIPU) return provider;
        } catch (IllegalArgumentException ignored) {
            // Normalize all unsupported provider values to one safe client error.
        }
        throw new IllegalArgumentException("不支持的 AI 供应商");
    }

    private String model(AiProvider provider) {
        return provider == AiProvider.ZHIPU ? properties.getZhipuModel() : properties.getDeepseekModelPro();
    }

    private String baseUrl(AiProvider provider) {
        return provider == AiProvider.ZHIPU ? properties.getZhipuBaseUrl() : properties.getDeepseekBaseUrl();
    }

    private String lastFour(String value) {
        return value.substring(Math.max(0, value.length() - 4));
    }

    private String currentUserUuid() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        return user == null || user.getUuid() == null ? "system" : user.getUuid();
    }
}
