package com.paper.mes.ai.process.prompt;

import com.paper.mes.ai.process.model.ProcessAiModelPrompt;

import java.util.List;

public record ProcessAiPromptBundle(
        ProcessAiModelPrompt prompt,
        List<String> memoryItemIds) {

    public ProcessAiPromptBundle {
        memoryItemIds = List.copyOf(memoryItemIds);
    }
}
