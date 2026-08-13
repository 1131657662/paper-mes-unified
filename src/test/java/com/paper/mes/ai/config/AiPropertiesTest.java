package com.paper.mes.ai.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiPropertiesTest {

    @Test
    void defaultsMatchApprovedZhipuLimits() {
        AiProperties properties = new AiProperties();

        assertThat(properties.getZhipuModel()).isEqualTo("glm-4.7-flash");
        assertThat(properties.getGlobalConcurrentRequests()).isEqualTo(1);
        assertThat(properties.getZhipuMaxOutputTokens()).isEqualTo(1_000);
    }

    @Test
    void unknownModeFailsClosed() {
        AiProperties properties = new AiProperties();
        properties.setDataMode("unexpected-mode");

        assertThat(properties.mode()).isEqualTo(AiDataMode.DISABLED);
        assertThat(properties.enabled()).isFalse();
    }
}
