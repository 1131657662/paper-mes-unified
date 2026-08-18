package com.paper.mes.ai.process.credential;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiProviderCredentialResolver {

    private final AiProviderSecretRepository repository;
    private final AiProviderSecretCipher cipher;
    private final AiProperties properties;

    @Transactional(readOnly = true)
    public Optional<String> resolveApiKey(AiProvider provider) {
        Optional<AiProviderSecretRow> stored = repository.find(provider.name());
        if (stored.isPresent()) return storedKey(provider, stored.orElseThrow());
        return nonBlank(environmentKey(provider));
    }

    @Transactional(readOnly = true)
    public AiProviderCredentialStatus status(AiProvider provider) {
        Optional<AiProviderSecretRow> stored = repository.find(provider.name());
        if (stored.isPresent()) return databaseStatus(provider, stored.orElseThrow());
        String environment = environmentKey(provider);
        boolean configured = environment != null && !environment.isBlank();
        return new AiProviderCredentialStatus(
                provider.name(), configured, configured ? "ENVIRONMENT" : "NONE",
                configured ? masked(environment) : null, configured, cipher.configured(),
                null, null);
    }

    private Optional<String> storedKey(AiProvider provider, AiProviderSecretRow row) {
        if (!row.enabled()) return Optional.empty();
        return nonBlank(cipher.decrypt(provider, row.ciphertext()));
    }

    private AiProviderCredentialStatus databaseStatus(AiProvider provider,
                                                       AiProviderSecretRow row) {
        boolean configured = row.enabled() && decryptable(provider, row);
        return new AiProviderCredentialStatus(
                provider.name(), configured, "DATABASE", maskedLastFour(row.lastFour()),
                row.enabled(), cipher.configured(), row.updatedBy(), row.updatedAt());
    }

    private boolean decryptable(AiProvider provider, AiProviderSecretRow row) {
        if (!cipher.configured()) return false;
        try {
            return nonBlank(cipher.decrypt(provider, row.ciphertext())).isPresent();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String environmentKey(AiProvider provider) {
        if (provider == AiProvider.DEEPSEEK) return properties.getDeepseekApiKey();
        if (provider == AiProvider.ZHIPU) return properties.getZhipuApiKey();
        return null;
    }

    private Optional<String> nonBlank(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    private String masked(String value) {
        String trimmed = value.trim();
        return maskedLastFour(trimmed.substring(Math.max(0, trimmed.length() - 4)));
    }

    private String maskedLastFour(String lastFour) {
        return lastFour == null || lastFour.isBlank() ? null : "****" + lastFour;
    }
}
