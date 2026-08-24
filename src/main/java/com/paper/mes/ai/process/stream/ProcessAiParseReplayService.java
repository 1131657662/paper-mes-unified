package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.compile.ProcessAiPlanCompilationService;
import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.ai.process.parse.ProcessAiParseStoreService;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class ProcessAiParseReplayService {

    private final ProcessAiParseStoreService parseStore;
    private final CloudDbContextReader contextReader;
    private final ProcessAiPlanCompilationService compilationService;
    private final ProcessAiParseResultSupport resultSupport;
    private final ProcessAiMessageService messageService;
    private final ProcessTextRedactor redactor;

    Optional<ProcessAiParseResultResponse> replay(String conversationId, String requestKey) {
        return parseStore.findReplay(conversationId, requestKey).map(record -> {
            var context = contextReader.read(record.orderUuid(), record.expectedVersion());
            if ("UNDERSTANDING".equals(record.resultKind())) {
                return resultSupport.replayUnderstanding(
                        record, resultSupport.readUnderstanding(record), context);
            }
            if ("FAILURE".equals(record.resultKind())) {
                return resultSupport.replayFailure(record, context);
            }
            var extraction = resultSupport.readExtraction(record);
            String original = messageService.restoreUserMessage(
                    record.orderUuid(), record.conversationId(), record.expectedVersion(),
                    record.requestIdempotencyKey());
            var compilation = compilationService.compile(
                    extraction, context, redactor.redact(original).charges());
            return resultSupport.replayed(record, extraction, compilation, context);
        });
    }
}
