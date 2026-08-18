package com.paper.mes.ai.process.model;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Explicit DeepSeek-primary and GLM-fallback process model route. */
@Primary
@Component
public class PrimaryFallbackProcessAiModelClient implements ProcessAiModelClient {

    private final ProcessAiModelClient primary;
    private final ProcessAiModelClient fallback;

    public PrimaryFallbackProcessAiModelClient(
            @Qualifier("deepSeekProcessClient") ProcessAiModelClient primary,
            @Qualifier("zhipuProcessClient") ProcessAiModelClient fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public ProcessAiModelResult parse(ProcessAiModelPrompt prompt, Consumer<String> deltaConsumer) {
        return parse(prompt, deltaConsumer, new ProcessAiCancellation());
    }

    @Override
    public ProcessAiModelResult parse(ProcessAiModelPrompt prompt, Consumer<String> deltaConsumer,
                                      ProcessAiCancellation cancellation) {
        AtomicBoolean emitted = new AtomicBoolean();
        try {
            return primary.parse(prompt, delta -> {
                emitted.set(true);
                deltaConsumer.accept(delta);
            }, cancellation);
        } catch (ProcessAiProviderException primaryFailure) {
            if (cancellation.isCancelled() || emitted.get()) throw primaryFailure;
            try {
                return fallback.parse(prompt, deltaConsumer, cancellation);
            } catch (ProcessAiProviderException fallbackFailure) {
                fallbackFailure.addSuppressed(primaryFailure);
                throw fallbackFailure;
            }
        }
    }
}
