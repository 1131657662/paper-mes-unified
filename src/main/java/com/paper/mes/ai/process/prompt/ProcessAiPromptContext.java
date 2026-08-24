package com.paper.mes.ai.process.prompt;

import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.intent.ProcessAiClarificationQuestion;
import com.paper.mes.ai.process.session.dto.ProcessAiMessageResponse;

import java.util.List;

public record ProcessAiPromptContext(
        String parseId,
        int parseRevision,
        ProcessAiOrderContext orderContext,
        ProjectMemorySnapshot memory,
        String sanitizedRequirement,
        List<ProcessAiMessageResponse> messages,
        ProcessAiClarificationQuestion clarificationQuestion,
        String answerCode,
        String answerText) {

    public ProcessAiPromptContext(String parseId, ProcessAiOrderContext orderContext,
                                  ProjectMemorySnapshot memory, String sanitizedRequirement,
                                  List<ProcessAiMessageResponse> messages) {
        this(parseId, 1, orderContext, memory, sanitizedRequirement, messages,
                null, null, null);
    }

    public ProcessAiPromptContext {
        messages = List.copyOf(messages);
    }
}
