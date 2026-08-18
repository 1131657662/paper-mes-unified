package com.paper.mes.ai.process.credential;

import java.time.LocalDateTime;

record AiProviderSecretRow(
        String provider,
        String ciphertext,
        String lastFour,
        boolean enabled,
        String updatedBy,
        LocalDateTime updatedAt) {
}
