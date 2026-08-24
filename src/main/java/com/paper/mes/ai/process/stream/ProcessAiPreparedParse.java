package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.intent.ProcessAiClarificationQuestion;
import com.paper.mes.ai.process.security.ProcessTextRedactionResult;
import com.paper.mes.ai.process.session.dto.ProcessAiMessageResponse;
import com.paper.mes.ai.process.session.dto.ProcessAiParseReservation;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseStreamRequest;

import java.util.List;

record ProcessAiPreparedParse(
        String orderUuid,
        String parseId,
        ProcessAiParseStreamRequest request,
        ProcessAiOrderContext orderContext,
        ProcessAiParseReservation reservation,
        ProjectMemorySnapshot memory,
        ProcessTextRedactionResult redaction,
        List<ProcessAiMessageResponse> messages,
        ProcessAiClarificationQuestion clarificationQuestion,
        int assistantSequence,
        long startedAtNanos) {

    ProcessAiPreparedParse {
        messages = List.copyOf(messages);
    }
}
