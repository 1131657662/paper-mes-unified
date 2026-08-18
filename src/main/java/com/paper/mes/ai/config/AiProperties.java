package com.paper.mes.ai.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String dataMode = AiDataMode.DISABLED.name();

    private String provider = AiProvider.LOCAL_RULES.name();

    @NotBlank
    private String rulesResource = "classpath:ai/rules-v1.0.0.json";

    private String memoryDir = "/etc/paper-mes/ai-memory";

    private String memorySeedResource = "classpath:ai/project-memory.seed.v1.json";

    @ToString.Exclude
    private String messageEncryptionKey = "";

    @ToString.Exclude
    private String configMasterKey = "";

    @Min(1_000)
    @Max(86_400_000)
    private long memoryCachePollMs = 60_000;

    @Min(100)
    @Max(2_000)
    private int maxQuestionChars = 1_000;

    @Min(1)
    @Max(4)
    private int globalConcurrentRequests = 1;

    @Min(2)
    @Max(100)
    private int memoryCandidateMinOrders = 5;

    @Min(1)
    @Max(90)
    private int memoryCandidateWindowDays = 7;

    @Min(7)
    @Max(365)
    private int memoryCandidateTtlDays = 90;

    @Min(1)
    @Max(1_440)
    private int memoryLearningProcessingTimeoutMinutes = 10;

    @ToString.Exclude
    private String deepseekApiKey = "";

    private String deepseekModelPro = "deepseek-v4-pro";

    private String deepseekModelFlash = "deepseek-v4-flash";

    private String deepseekBaseUrl = "https://api.deepseek.com";

    @Min(100)
    @Max(30_000)
    private int deepseekConnectTimeoutMs = 3_000;

    @Min(1_000)
    @Max(300_000)
    private int deepseekReadTimeoutMs = 120_000;

    @Min(128)
    @Max(16_000)
    private int deepseekMaxOutputTokens = 4_000;

    @Min(1)
    @Max(3)
    private int providerMaxAttempts = 2;

    @Min(0)
    @Max(5_000)
    private long providerRetryBackoffMs = 250;

    @Min(2)
    @Max(20)
    private int providerCircuitFailureThreshold = 3;

    @Min(1)
    @Max(300)
    private int providerCircuitOpenSeconds = 30;

    @Min(10_000)
    @Max(3_600_000)
    private long conversationCleanupDelayMs = 60_000;

    @Min(1_000)
    @Max(1_350)
    private int defaultTargetDiameterMm = 1_200;

    @ToString.Exclude
    private String zhipuApiKey = "";

    private String zhipuModel = "glm-4.7-flash";

    private String zhipuBaseUrl = "https://open.bigmodel.cn";

    @Min(100)
    @Max(10_000)
    private int zhipuConnectTimeoutMs = 1_000;

    @Min(100)
    @Max(30_000)
    private int zhipuReadTimeoutMs = 8_000;

    @Min(128)
    @Max(4_000)
    private int zhipuMaxOutputTokens = 1_000;

    public boolean enabled() {
        return mode() != AiDataMode.DISABLED;
    }

    public AiDataMode mode() {
        try {
            return AiDataMode.valueOf(dataMode == null ? "" : dataMode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return AiDataMode.DISABLED;
        }
    }

    public boolean zhipuConfigured() {
        return providerMode() == AiProvider.ZHIPU && !blank(zhipuApiKey)
                && !blank(zhipuModel) && !blank(zhipuBaseUrl);
    }

    public boolean zhipuProcessEndpointConfigured() {
        return !blank(zhipuModel) && !blank(zhipuBaseUrl);
    }

    public AiProvider effectiveProvider() {
        if (deepseekConfigured()) return AiProvider.DEEPSEEK;
        return zhipuConfigured() ? AiProvider.ZHIPU : AiProvider.LOCAL_RULES;
    }

    public boolean deepseekConfigured() {
        return providerMode() == AiProvider.DEEPSEEK && !blank(deepseekApiKey)
                && !blank(deepseekModelPro) && !blank(deepseekBaseUrl);
    }

    public boolean deepseekProcessEndpointConfigured() {
        return !blank(deepseekModelPro) && !blank(deepseekBaseUrl);
    }

    public AiProvider providerMode() {
        try {
            return AiProvider.valueOf(provider == null ? "" : provider.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return AiProvider.LOCAL_RULES;
        }
    }

    public long processStreamTimeoutMs() {
        long attempts = Math.max(1, providerMaxAttempts);
        long retryBudget = Math.max(0, attempts - 1) * providerRetryBackoffMs;
        long routeBudget = deepseekReadTimeoutMs + deepseekConnectTimeoutMs
                + zhipuReadTimeoutMs + zhipuConnectTimeoutMs;
        long providerBudget = attempts * routeBudget + retryBudget;
        return Math.max(60_000L, Math.min(3_600_000L, providerBudget + 30_000L));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
