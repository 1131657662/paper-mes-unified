package com.paper.mes.ai.process.model;

import com.paper.mes.ai.config.AiProperties;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ProcessAiProviderCircuitBreakerTest {

    @Test
    void beforeCallRejectsRequestsAfterConfiguredFailureThreshold() {
        AiProperties properties = properties();
        ProcessAiProviderCircuitBreaker breaker = new ProcessAiProviderCircuitBreaker(
                properties, () -> 1_000L);

        breaker.failure(retryableFailure());
        breaker.failure(retryableFailure());
        breaker.failure(retryableFailure());

        ProcessAiProviderException error = catchThrowableOfType(
                breaker::beforeCall, ProcessAiProviderException.class);
        assertThat(error.failureCode()).isEqualTo("AI_PROVIDER_CIRCUIT_OPEN");
    }

    @Test
    void beforeCallAllowsProbeAfterOpenPeriodExpires() {
        AiProperties properties = properties();
        AtomicLong now = new AtomicLong(1_000L);
        ProcessAiProviderCircuitBreaker breaker = new ProcessAiProviderCircuitBreaker(
                properties, now::get);
        breaker.failure(retryableFailure());
        breaker.failure(retryableFailure());
        breaker.failure(retryableFailure());

        now.addAndGet(30_000_000_000L);

        assertThatCode(breaker::beforeCall).doesNotThrowAnyException();
    }

    @Test
    void circuitOpenFailuresDoNotExtendTheOpenWindow() {
        AiProperties properties = properties();
        AtomicLong now = new AtomicLong(1_000L);
        ProcessAiProviderCircuitBreaker breaker = new ProcessAiProviderCircuitBreaker(
                properties, now::get);
        breaker.failure(retryableFailure());
        breaker.failure(retryableFailure());
        breaker.failure(retryableFailure());

        now.addAndGet(29_000_000_000L);
        ProcessAiProviderException open = new ProcessAiProviderException(
                "AI_PROVIDER_CIRCUIT_OPEN", true, "open");
        breaker.failure(open);
        now.addAndGet(2_000_000_000L);

        assertThatCode(breaker::beforeCall).doesNotThrowAnyException();
    }

    private AiProperties properties() {
        AiProperties properties = new AiProperties();
        properties.setProviderCircuitFailureThreshold(3);
        properties.setProviderCircuitOpenSeconds(30);
        return properties;
    }

    private ProcessAiProviderException retryableFailure() {
        return new ProcessAiProviderException("AI_PROVIDER_TIMEOUT", true, "timeout");
    }
}
