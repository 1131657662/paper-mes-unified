package com.paper.mes.ai.process.credential.dto;

import java.time.LocalDateTime;

public record AiProviderSettingsResponse(
        String provider,
        String model,
        String baseUrl,
        boolean configured,
        String source,
        String maskedApiKey,
        boolean enabled,
        boolean databaseStorageReady,
        String updatedBy,
        LocalDateTime updatedAt) {
}
