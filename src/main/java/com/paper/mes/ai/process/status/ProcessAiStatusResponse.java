package com.paper.mes.ai.process.status;

public record ProcessAiStatusResponse(
        boolean enabled,
        boolean ready,
        String provider,
        String model,
        boolean providerConfigured,
        String fallbackProvider,
        String fallbackModel,
        boolean fallbackConfigured,
        boolean messageEncryptionReady,
        String projectMemoryState,
        String unavailableReason) {
}
