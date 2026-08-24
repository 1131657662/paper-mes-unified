package com.paper.mes.ai.memory.candidate;

import com.paper.mes.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
class ProcessAiMemoryReferenceHasher {

    private final AiProperties properties;

    boolean isConfigured() {
        return properties.memoryReferenceHmacConfigured();
    }

    String hash(String value) {
        if (value == null) return null;
        if (!isConfigured()) {
            throw new IllegalStateException("memory reference HMAC key is not configured");
        }
        String key = properties.getMemoryReferenceHmacKey();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("memory reference HMAC is unavailable", ex);
        }
    }
}
