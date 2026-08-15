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

    @Min(1_000)
    @Max(86_400_000)
    private long memoryCachePollMs = 60_000;

    @Min(100)
    @Max(2_000)
    private int maxQuestionChars = 1_000;

    @Min(1)
    @Max(4)
    private int globalConcurrentRequests = 1;

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

    public AiProvider effectiveProvider() {
        return zhipuConfigured() ? AiProvider.ZHIPU : AiProvider.LOCAL_RULES;
    }

    public AiProvider providerMode() {
        try {
            return AiProvider.valueOf(provider == null ? "" : provider.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return AiProvider.LOCAL_RULES;
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
