package com.paper.mes.ai.process.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrimaryFallbackProcessAiModelClientTest {

    @Test
    void parse_primarySuccessDoesNotCallFallback() {
        AtomicInteger fallbackCalls = new AtomicInteger();
        ProcessAiModelResult primaryResult = result("DEEPSEEK", "primary");
        ProcessAiModelClient primary = (prompt, consumer) -> primaryResult;
        ProcessAiModelClient fallback = (prompt, consumer) -> {
            fallbackCalls.incrementAndGet();
            return result("ZHIPU", "fallback");
        };

        ProcessAiModelResult actual = router(primary, fallback).parse(prompt(), ignored -> { });

        assertThat(actual).isSameAs(primaryResult);
        assertThat(fallbackCalls).hasValue(0);
    }

    @Test
    void parse_primaryFailureBeforeOutputUsesGlmFallback() {
        ProcessAiModelClient primary = (prompt, consumer) -> {
            throw failure("primary");
        };
        ProcessAiModelClient fallback = (prompt, consumer) -> {
            consumer.accept("fallback");
            return result("ZHIPU", "fallback");
        };
        List<String> deltas = new ArrayList<>();

        ProcessAiModelResult actual = router(primary, fallback).parse(prompt(), deltas::add);

        assertThat(actual.provider()).isEqualTo("ZHIPU");
        assertThat(deltas).containsExactly("fallback");
    }

    @Test
    void parse_primaryFailureAfterOutputNeverMixesFallback() {
        AtomicInteger fallbackCalls = new AtomicInteger();
        ProcessAiProviderException failure = failure("primary");
        ProcessAiModelClient primary = (prompt, consumer) -> {
            consumer.accept("partial");
            throw failure;
        };
        ProcessAiModelClient fallback = (prompt, consumer) -> {
            fallbackCalls.incrementAndGet();
            return result("ZHIPU", "fallback");
        };

        assertThatThrownBy(() -> router(primary, fallback).parse(prompt(), ignored -> { }))
                .isSameAs(failure);
        assertThat(fallbackCalls).hasValue(0);
    }

    @Test
    void parse_bothFailuresReturnsFallbackFailureWithPrimarySuppressed() {
        ProcessAiProviderException primaryFailure = failure("primary");
        ProcessAiProviderException fallbackFailure = failure("fallback");
        ProcessAiModelClient primary = (prompt, consumer) -> { throw primaryFailure; };
        ProcessAiModelClient fallback = (prompt, consumer) -> { throw fallbackFailure; };

        assertThatThrownBy(() -> router(primary, fallback).parse(prompt(), ignored -> { }))
                .isSameAs(fallbackFailure);
        assertThat(fallbackFailure.getSuppressed()).containsExactly(primaryFailure);
    }

    @Test
    void parseFallbackUsesFallbackRouteAfterPrimaryContractFailure() {
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger fallbackCalls = new AtomicInteger();
        ProcessAiModelClient primary = (prompt, consumer) -> {
            primaryCalls.incrementAndGet();
            return result("DEEPSEEK", "invalid");
        };
        ProcessAiModelClient fallback = (prompt, consumer) -> {
            fallbackCalls.incrementAndGet();
            return result("ZHIPU", "valid");
        };

        ProcessAiModelResult actual = router(primary, fallback)
                .parseFallback(prompt(), ignored -> { }, new ProcessAiCancellation());

        assertThat(actual.provider()).isEqualTo("ZHIPU");
        assertThat(primaryCalls).hasValue(0);
        assertThat(fallbackCalls).hasValue(1);
    }

    private PrimaryFallbackProcessAiModelClient router(ProcessAiModelClient primary,
                                                       ProcessAiModelClient fallback) {
        return new PrimaryFallbackProcessAiModelClient(primary, fallback);
    }

    private ProcessAiProviderException failure(String message) {
        return new ProcessAiProviderException("AI_PROVIDER_UNAVAILABLE", true, message);
    }

    private ProcessAiModelPrompt prompt() {
        return new ProcessAiModelPrompt("system", "context");
    }

    private ProcessAiModelResult result(String provider, String content) {
        return new ProcessAiModelResult(content, "model", provider, "PRO", 1, 1);
    }
}
