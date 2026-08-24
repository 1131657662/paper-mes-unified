package com.paper.mes.ai.process.model;

import com.paper.mes.ai.config.AiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Component
public class ProcessAiModelRetryExecutor {

    private final ProcessAiModelClient client;
    private final ProcessAiProviderCircuitBreaker circuitBreaker;
    private final AiProperties properties;
    private final Sleeper sleeper;

    @Autowired
    public ProcessAiModelRetryExecutor(
            ProcessAiModelClient client,
            ProcessAiProviderCircuitBreaker circuitBreaker,
            AiProperties properties) {
        this(client, circuitBreaker, properties, Thread::sleep);
    }

    ProcessAiModelRetryExecutor(ProcessAiModelClient client,
                                ProcessAiProviderCircuitBreaker circuitBreaker,
                                AiProperties properties, Sleeper sleeper) {
        this.client = client;
        this.circuitBreaker = circuitBreaker;
        this.properties = properties;
        this.sleeper = sleeper;
    }

    public ProcessAiModelResult parse(
            ProcessAiModelPrompt prompt, Consumer<String> deltaConsumer) {
        return parse(prompt, deltaConsumer, new ProcessAiCancellation());
    }

    public ProcessAiModelResult parse(ProcessAiModelPrompt prompt, Consumer<String> deltaConsumer,
                                      ProcessAiCancellation cancellation) {
        return parseWithRoute(prompt, deltaConsumer, cancellation, false);
    }

    public ProcessAiModelResult parseFallback(ProcessAiModelPrompt prompt,
                                               Consumer<String> deltaConsumer,
                                               ProcessAiCancellation cancellation) {
        return parseWithRoute(prompt, deltaConsumer, cancellation, true);
    }

    private ProcessAiModelResult parseWithRoute(ProcessAiModelPrompt prompt,
                                                Consumer<String> deltaConsumer,
                                                ProcessAiCancellation cancellation,
                                                boolean fallbackRoute) {
        ProcessAiProviderException last = null;
        for (int attempt = 1; attempt <= properties.getProviderMaxAttempts(); attempt++) {
            AtomicBoolean streamed = new AtomicBoolean();
            try {
                cancellation.throwIfCancelled();
                circuitBreaker.beforeCall();
                ProcessAiModelResult result = invoke(client, fallbackRoute, prompt, delta -> {
                    streamed.set(true);
                    deltaConsumer.accept(delta);
                }, cancellation);
                circuitBreaker.success();
                return result;
            } catch (ProcessAiProviderException exception) {
                circuitBreaker.failure(exception);
                last = exception;
                if (cancellation.isCancelled()
                        || !retryable(exception, streamed.get(), attempt)) throw exception;
                backoff(attempt);
            }
        }
        throw last == null ? new IllegalStateException("AI provider retry failed") : last;
    }

    private ProcessAiModelResult invoke(ProcessAiModelClient target, boolean fallbackRoute,
                                        ProcessAiModelPrompt prompt,
                                        java.util.function.Consumer<String> deltaConsumer,
                                        ProcessAiCancellation cancellation) {
        return fallbackRoute
                ? target.parseFallback(prompt, deltaConsumer, cancellation)
                : target.parse(prompt, deltaConsumer, cancellation);
    }

    private boolean retryable(ProcessAiProviderException exception,
                              boolean streamed, int attempt) {
        return exception.retryable() && !streamed
                && attempt < properties.getProviderMaxAttempts();
    }

    private void backoff(int attempt) {
        try {
            sleeper.sleep(properties.getProviderRetryBackoffMs() * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ProcessAiProviderException(
                    "AI_PROVIDER_INTERRUPTED", true, "AI provider retry was interrupted");
        }
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long milliseconds) throws InterruptedException;
    }
}
