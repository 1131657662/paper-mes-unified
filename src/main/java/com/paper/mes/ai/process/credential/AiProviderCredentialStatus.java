package com.paper.mes.ai.process.credential;

import java.time.LocalDateTime;

public record AiProviderCredentialStatus(
        String provider,
        boolean configured,
        String source,
        String maskedApiKey,
        boolean enabled,
        boolean databaseStorageReady,
        String updatedBy,
        LocalDateTime updatedAt) {
}
