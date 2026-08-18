package com.paper.mes.ai.process.model;

import com.paper.mes.ai.config.AiProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessAiModelRetryExecutorTest {

    @Test
    void parseRetriesRetryableFailureBeforeAnyStreamOutput() {
        AtomicInteger calls = new AtomicInteger();
        ProcessAiModelResult success = result("complete");
        ProcessAiModelClient client = (prompt, consumer) -> {
            if (calls.incrementAndGet() == 1) throw retryableFailure();
            consumer.accept("complete");
            return success;
        };
        ProcessAiModelRetryExecutor executor = executor(client, new AiProperties());
        List<String> deltas = new ArrayList<>();

        ProcessAiModelResult actual = executor.parse(prompt(), deltas::add);

        assertThat(actual).isSameAs(success);
        assertThat(calls).hasValue(2);
        assertThat(deltas).containsExactly("complete");
    }

    @Test
    void parseDoesNotRetryAfterPartialStreamOutput() {
        AtomicInteger calls = new AtomicInteger();
        ProcessAiProviderException failure = retryableFailure();
        ProcessAiModelClient client = (prompt, consumer) -> {
            calls.incrementAndGet();
            consumer.accept("partial");
            throw failure;
        };
        ProcessAiModelRetryExecutor executor = executor(client, new AiProperties());
        List<String> deltas = new ArrayList<>();

        assertThatThrownBy(() -> executor.parse(prompt(), deltas::add)).isSameAs(failure);

        assertThat(calls).hasValue(1);
        assertThat(deltas).containsExactly("partial");
    }

    private ProcessAiModelRetryExecutor executor(ProcessAiModelClient client,
                                                  AiProperties properties) {
        ProcessAiProviderCircuitBreaker breaker = new ProcessAiProviderCircuitBreaker(
                properties, System::nanoTime);
        return new ProcessAiModelRetryExecutor(client, breaker, properties, ignored -> {
        });
    }

    private ProcessAiProviderException retryableFailure() {
        return new ProcessAiProviderException("AI_PROVIDER_TIMEOUT", true, "timeout");
    }

    private ProcessAiModelPrompt prompt() {
        return new ProcessAiModelPrompt("system", "context");
    }

    private ProcessAiModelResult result(String content) {
        return new ProcessAiModelResult(
                content, "deepseek-v4-pro", "DEEPSEEK", "PRO", 10, 5);
    }
}
