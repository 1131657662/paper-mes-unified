package com.paper.mes.ai.process.status;

import com.paper.mes.ai.config.AiDataMode;
import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import com.paper.mes.ai.memory.ProjectMemoryDocumentProvider;
import com.paper.mes.ai.process.credential.AiProviderCredentialResolver;
import com.paper.mes.ai.process.session.crypto.AiMessageCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessAiStatusService {

    private final AiProperties properties;
    private final ProjectMemoryDocumentProvider memoryProvider;
    private final AiMessageCipher messageCipher;
    private final AiProviderCredentialResolver credentialResolver;

    public ProcessAiStatusResponse status() {
        boolean enabled = properties.mode() == AiDataMode.CONTEXT_ALLOWLIST;
        boolean primary = properties.deepseekProcessEndpointConfigured()
                && credentialResolver.resolveApiKey(AiProvider.DEEPSEEK).isPresent();
        boolean fallback = properties.zhipuProcessEndpointConfigured()
                && credentialResolver.resolveApiKey(AiProvider.ZHIPU).isPresent();
        boolean provider = primary || fallback;
        boolean encryption = messageCipher.configured();
        boolean referenceHmac = properties.memoryReferenceHmacConfigured();
        boolean memory = memoryProvider.ready();
        boolean ready = enabled && provider && encryption && referenceHmac && memory;
        return new ProcessAiStatusResponse(enabled, ready, "DEEPSEEK",
                properties.getDeepseekModelPro(), primary, "ZHIPU",
                properties.getZhipuModel(), fallback, encryption,
                memoryProvider.state(), ready ? null
                        : reason(enabled, provider, encryption, referenceHmac, memory));
    }

    private String reason(boolean enabled, boolean provider, boolean encryption,
                          boolean referenceHmac, boolean memory) {
        if (!enabled) return "CLOUD_AI_DISABLED";
        if (!provider) return "AI_PROVIDER_NOT_CONFIGURED";
        if (!encryption) return "AI_MESSAGE_KEY_UNAVAILABLE";
        if (!referenceHmac) return "AI_MEMORY_REFERENCE_HMAC_UNAVAILABLE";
        if (!memory) return "AI_MEMORY_UNAVAILABLE";
        return "AI_PROCESS_UNAVAILABLE";
    }
}
