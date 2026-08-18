package com.paper.mes.ai.process.model;

import com.paper.mes.ai.config.AiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.LongSupplier;

@Component
public class ProcessAiProviderCircuitBreaker {

    private final AiProperties properties;
    private final LongSupplier nanoTime;
    private int consecutiveFailures;
    private long openUntilNanos;

    @Autowired
    public ProcessAiProviderCircuitBreaker(AiProperties properties) {
        this(properties, System::nanoTime);
    }

    ProcessAiProviderCircuitBreaker(AiProperties properties, LongSupplier nanoTime) {
        this.properties = properties;
        this.nanoTime = nanoTime;
    }

    public synchronized void beforeCall() {
        long now = nanoTime.getAsLong();
        if (openUntilNanos == 0) return;
        if (now >= openUntilNanos) {
            openUntilNanos = 0;
            consecutiveFailures = 0;
            return;
        }
        throw new ProcessAiProviderException(
                "AI_PROVIDER_CIRCUIT_OPEN", true, "AI provider is temporarily unavailable");
    }

    public synchronized void success() {
        consecutiveFailures = 0;
        openUntilNanos = 0;
    }

    public synchronized void failure(ProcessAiProviderException exception) {
        if (!exception.retryable() || "AI_PROVIDER_CIRCUIT_OPEN".equals(exception.failureCode())) {
            return;
        }
        consecutiveFailures++;
        if (consecutiveFailures < properties.getProviderCircuitFailureThreshold()) return;
        openUntilNanos = nanoTime.getAsLong()
                + properties.getProviderCircuitOpenSeconds() * 1_000_000_000L;
    }
}
