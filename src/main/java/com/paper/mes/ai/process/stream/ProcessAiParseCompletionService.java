package com.paper.mes.ai.process.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.compile.ProcessAiDefaultResolver;
import com.paper.mes.ai.process.compile.ProcessAiDefaultValue;
import com.paper.mes.ai.process.compile.ProcessAiPreviewHashInput;
import com.paper.mes.ai.process.compile.ProcessAiPreviewHashService;
import com.paper.mes.ai.process.compile.ProcessAiPlanCompilationService;
import com.paper.mes.ai.process.parse.ProcessAiParseRecord;
import com.paper.mes.ai.process.parse.ProcessAiClarificationValidator;
import com.paper.mes.ai.process.parse.ProcessAiParseStoreCommand;
import com.paper.mes.ai.process.parse.ProcessAiParseStoreService;
import com.paper.mes.ai.process.intent.ProcessAiClarificationQuestionMapper;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.session.dto.UpdateAssistantMessageCommand;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
class ProcessAiParseCompletionService {

    private final ProcessAiParseStoreService parseStore;
    private final ProcessAiMessageService messageService;
    private final ProcessAiPlanCompilationService compilationService;
    private final ProcessAiParseResultSupport resultSupport;
    private final ProcessAiParseAuditRecorder auditRecorder;
    private final ProcessAiDefaultResolver defaultResolver;
    private final ProcessAiPreviewHashService previewHashService;
    private final ObjectMapper objectMapper;
    private final ProcessAiClarificationValidator clarificationValidator;

    @Transactional
    ProcessAiParseResultResponse complete(ProcessAiPreparedParse prepared,
                                          ProcessAiModelExecution execution) {
        boolean unknownAnswer = clarificationValidator.isUnknown(
                prepared.request().action().equals("CLARIFY")
                        ? (prepared.request().answerCode() != null
                        ? prepared.request().answerCode() : prepared.request().answerText()) : null);
        ProcessAiModelExecution completedExecution = ensureClarification(execution,
                prepared.reservation().parseRevision(), unknownAnswer);
        boolean blocked = unknownAnswer || blocking(completedExecution.extraction());
        ProcessAiCompilationResult compilation = blocked
                ? blockedUnknownAnswer()
                : compilationService.compile(completedExecution.extraction(), prepared.orderContext(),
                        prepared.redaction().charges());
        String status = resultSupport.status(completedExecution.extraction(), compilation);
        if (unknownAnswer) status = "CLARIFICATION";
        boolean previewReady = "READY".equals(status);
        List<ProcessAiDefaultValue> defaults = !previewReady ? List.of()
                : defaultResolver.resolve(execution.extraction(), prepared.orderContext());
        String previewHash = !previewReady ? null
                : previewHash(completedExecution, prepared, compilation, defaults);
        ProcessAiParseResultResponse response = resultSupport.completed(
                prepared, completedExecution, status, compilation, previewHash,
                defaults.stream().map(ProcessAiDefaultValue::defaultId).toList());
        ProcessAiParseRecord record = store(prepared, completedExecution, status, previewHash, defaults);
        auditRecorder.successAfterCommit(new ProcessAiParseAuditSuccess(
                prepared, completedExecution, record, status));
        updateAssistant(prepared, resultSupport.summary(completedExecution, status), response);
        return response;
    }

    private ProcessAiModelExecution ensureClarification(ProcessAiModelExecution execution,
                                                        int parseRevision, boolean unknownAnswer) {
        ProcessAiExtractionResult extraction = execution.extraction();
        boolean blocked = unknownAnswer || blocking(extraction);
        if (!blocked || !extraction.clarificationQuestions().isEmpty()) return execution;
        var revised = new com.paper.mes.ai.process.intent.ProcessAiExtractionResult(
                extraction.parseId(), extraction.schemaVersion(), extraction.assignments(),
                extraction.unmappedText(), extraction.conflicts(), true,
                List.of(unknownAnswer
                        ? "用户暂不确定，请补充可以确认的工艺信息或转人工处理"
                        : "部分工艺依据无法由服务端核验，请补充客户要求或转人工处理"));
        return new ProcessAiModelExecution(execution.promptBundle(), execution.modelResult(), revised);
    }

    private boolean blocking(com.paper.mes.ai.process.intent.ProcessAiExtractionResult extraction) {
        return extraction.needsClarification() || !extraction.conflicts().isEmpty()
                || !extraction.unmappedText().isEmpty()
                || !extraction.clarificationQuestions().isEmpty();
    }

    private ProcessAiCompilationResult blockedUnknownAnswer() {
        return new ProcessAiCompilationResult(false, List.of(), List.of(),
                List.of("用户选择了不确定，不能直接生成工艺预览"), List.of());
    }

    @Transactional
    ProcessAiParseResultResponse completeUnderstanding(ProcessAiPreparedParse prepared,
                                                       ProcessAiModelExecution execution) {
        ProcessAiParseRecord record = parseStore.store(new ProcessAiParseStoreCommand(
                prepared.orderUuid(), prepared.request().conversationId(),
                prepared.request().expectedVersion(), prepared.reservation().parseRevision(),
                prepared.reservation().memoryGeneration(), prepared.request().idempotencyKey(),
                "CLARIFICATION", prepared.memory(), execution.promptBundle().memoryItemIds(),
                execution.modelResult(), execution.understanding(), "CLARIFYING", "UNDERSTANDING",
                2, resultSupport.json(execution.understanding().clarificationQuestions()),
                inputHash(prepared), contextHash(prepared), null, null, null, null, null));
        ProcessAiParseResultResponse response = resultSupport.understood(prepared, execution);
        auditRecorder.successAfterCommit(new ProcessAiParseAuditSuccess(
                prepared, execution, record, "CLARIFICATION"));
        updateAssistant(prepared, resultSupport.summary(execution, "CLARIFICATION"), response);
        return response;
    }

    private ProcessAiParseRecord store(ProcessAiPreparedParse prepared,
                                       ProcessAiModelExecution execution, String status,
                                       String previewHash, List<ProcessAiDefaultValue> defaults) {
        return parseStore.store(new ProcessAiParseStoreCommand(
                prepared.orderUuid(), prepared.request().conversationId(),
                prepared.request().expectedVersion(), prepared.reservation().parseRevision(),
                prepared.reservation().memoryGeneration(), prepared.request().idempotencyKey(),
                status, prepared.memory(),
                execution.promptBundle().memoryItemIds(), execution.modelResult(),
                execution.extraction(), null,
                dialogueState(status), "EXTRACTION", 2,
                null, clarificationJson(execution, prepared.reservation().parseRevision()), null,
                inputHash(prepared), contextHash(prepared), previewHash, null, null,
                resultSupport.json(defaults.stream().map(ProcessAiDefaultValue::defaultId).toList()), null,
                null));
    }

    private String dialogueState(String status) {
        return switch (status) {
            case "CLARIFICATION" -> "CLARIFYING";
            case "READY" -> "PREVIEW_READY";
            default -> "COMPLETED";
        };
    }

    private String clarificationJson(ProcessAiModelExecution execution, int parseRevision) {
        if (execution.extraction().clarificationQuestions().isEmpty()) return null;
        return resultSupport.json(ProcessAiClarificationQuestionMapper.fromExtraction(
                execution.extraction().clarificationQuestions(), parseRevision));
    }

    private String inputHash(ProcessAiPreparedParse prepared) {
        return ProcessAiAuditHasher.sha256(prepared.redaction().sanitizedText());
    }

    private String contextHash(ProcessAiPreparedParse prepared) {
        return ProcessAiAuditHasher.sha256(prepared.orderUuid(),
                Integer.toString(prepared.request().expectedVersion()),
                Integer.toString(prepared.reservation().memoryGeneration()),
                prepared.memory().docVersion(), prepared.memory().checksum());
    }

    private String previewHash(ProcessAiModelExecution execution, ProcessAiPreparedParse prepared,
                               ProcessAiCompilationResult compilation,
                               List<ProcessAiDefaultValue> defaults) {
        String extractionJson = resultSupport.json(execution.extraction());
        try {
            return previewHashService.hash(new ProcessAiPreviewHashInput(
                    prepared.orderUuid(), prepared.request().expectedVersion(),
                    prepared.request().conversationId(), prepared.reservation().memoryGeneration(),
                    prepared.memory().docVersion(), prepared.memory().checksum(),
                    ProcessAiAuditHasher.sha256(extractionJson),
                    ProcessAiAuditHasher.sha256(extractionJson), null,
                    defaults.stream().map(ProcessAiDefaultValue::defaultId).toList(),
                    objectMapper.readTree(resultSupport.json(compilation.rollConfigurations())),
                    objectMapper.readTree(resultSupport.json(compilation.plans())),
                    objectMapper.readTree(resultSupport.json(compilation.packagingCandidates()))));
        } catch (Exception ex) {
            throw new IllegalStateException("AI preview hash input is invalid", ex);
        }
    }

    private void updateAssistant(ProcessAiPreparedParse prepared, String summary,
                                 ProcessAiParseResultResponse response) {
        messageService.updateAssistant(new UpdateAssistantMessageCommand(
                prepared.orderUuid(), prepared.request().conversationId(),
                prepared.request().expectedVersion(), prepared.assistantSequence(),
                summary, "FINAL", resultSupport.json(response)));
    }
}
