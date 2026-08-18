package com.paper.mes.ai.process.model;

import java.util.function.Consumer;

public interface ProcessAiModelClient {

    ProcessAiModelResult parse(ProcessAiModelPrompt prompt, Consumer<String> deltaConsumer);

    default ProcessAiModelResult parse(ProcessAiModelPrompt prompt, Consumer<String> deltaConsumer,
                                       ProcessAiCancellation cancellation) {
        cancellation.throwIfCancelled();
        return parse(prompt, deltaConsumer);
    }
}
