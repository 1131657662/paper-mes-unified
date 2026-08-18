package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.compile.ProcessAiPlanCompilationService;
import com.paper.mes.ai.process.parse.ProcessAiParseRecord;
import com.paper.mes.ai.process.parse.ProcessAiParseStoreCommand;
import com.paper.mes.ai.process.parse.ProcessAiParseStoreService;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.session.dto.UpdateAssistantMessageCommand;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ProcessAiParseCompletionService {

    private final ProcessAiParseStoreService parseStore;
    private final ProcessAiMessageService messageService;
    private final ProcessAiPlanCompilationService compilationService;
    private final ProcessAiParseResultSupport resultSupport;
    private final ProcessAiParseAuditRecorder auditRecorder;

    @Transactional
    ProcessAiParseResultResponse complete(ProcessAiPreparedParse prepared,
                                          ProcessAiModelExecution execution) {
        ProcessAiCompilationResult compilation = compilationService.compile(
                execution.extraction(), prepared.orderContext(), prepared.redaction().charges());
        String status = resultSupport.status(execution.extraction(), compilation);
        ProcessAiParseResultResponse response = resultSupport.completed(
                prepared, execution, status, compilation);
        ProcessAiParseRecord record = store(prepared, execution, status);
        auditRecorder.successAfterCommit(new ProcessAiParseAuditSuccess(
                prepared, execution, record, status));
        updateAssistant(prepared, resultSupport.summary(execution, status), response);
        return response;
    }

    private ProcessAiParseRecord store(ProcessAiPreparedParse prepared,
                                       ProcessAiModelExecution execution, String status) {
        return parseStore.store(new ProcessAiParseStoreCommand(
                prepared.orderUuid(), prepared.request().conversationId(),
                prepared.request().expectedVersion(), prepared.reservation().parseRevision(),
                prepared.reservation().memoryGeneration(), prepared.request().idempotencyKey(),
                status, prepared.memory(),
                execution.promptBundle().memoryItemIds(), execution.modelResult(),
                execution.extraction()));
    }

    private void updateAssistant(ProcessAiPreparedParse prepared, String summary,
                                 ProcessAiParseResultResponse response) {
        messageService.updateAssistant(new UpdateAssistantMessageCommand(
                prepared.orderUuid(), prepared.request().conversationId(),
                prepared.request().expectedVersion(), prepared.assistantSequence(),
                summary, "FINAL", resultSupport.json(response)));
    }
}
