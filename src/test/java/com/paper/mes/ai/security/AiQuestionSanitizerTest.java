package com.paper.mes.ai.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiQuestionSanitizerTest {

    private final AiQuestionSanitizer sanitizer = new AiQuestionSanitizer();

    @Test
    void businessIdentifierIsRejectedBeforeOutboundProcessing() {
        var result = sanitizer.inspect("订单 123456 的状态为什么不能操作？");

        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).contains("敏感");
    }

    @Test
    void ordinaryRuleQuestionRemainsAvailableLocally() {
        var result = sanitizer.inspect("E001 为什么不能操作？");

        assertThat(result.allowed()).isTrue();
        assertThat(result.sanitizedQuestion()).isEqualTo("E001 为什么不能操作？");
    }
}
