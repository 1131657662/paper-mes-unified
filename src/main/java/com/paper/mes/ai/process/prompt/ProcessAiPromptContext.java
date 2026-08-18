package com.paper.mes.ai.process.prompt;

import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.session.dto.ProcessAiMessageResponse;

import java.util.List;

public record ProcessAiPromptContext(
        String parseId,
        ProcessAiOrderContext orderContext,
        ProjectMemorySnapshot memory,
        String sanitizedRequirement,
        List<ProcessAiMessageResponse> messages) {

    public ProcessAiPromptContext {
        messages = List.copyOf(messages);
    }
}
