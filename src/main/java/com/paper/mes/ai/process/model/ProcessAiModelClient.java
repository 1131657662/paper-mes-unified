package com.paper.mes.ai.process.model;

import java.util.function.Consumer;

public interface ProcessAiModelClient {

    ProcessAiModelResult parse(ProcessAiModelPrompt prompt, Consumer<String> deltaConsumer);

    default ProcessAiModelResult parse(ProcessAiModelPrompt prompt, Consumer<String> deltaConsumer,
                                       ProcessAiCancellation cancellation) {
        cancellation.throwIfCancelled();
        return parse(prompt, deltaConsumer);
    }

    /** Executes the configured fallback route when the primary result violates the output contract. */
    default ProcessAiModelResult parseFallback(ProcessAiModelPrompt prompt,
                                                Consumer<String> deltaConsumer,
                                                ProcessAiCancellation cancellation) {
        return parse(prompt, deltaConsumer, cancellation);
    }
}
