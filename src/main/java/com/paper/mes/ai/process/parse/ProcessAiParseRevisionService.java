package com.paper.mes.ai.process.parse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.compile.ProcessAiDefaultResolver;
import com.paper.mes.ai.process.compile.ProcessAiDefaultValue;
import com.paper.mes.ai.process.compile.ProcessAiPlanCompilationService;
import com.paper.mes.ai.process.compile.ProcessAiPreviewHashInput;
import com.paper.mes.ai.process.compile.ProcessAiPreviewHashService;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.intent.ProcessAiClarificationQuestionMapper;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.parse.dto.ProcessAiCorrection;
import com.paper.mes.ai.process.parse.dto.ProcessAiReviseRequest;
import com.paper.mes.ai.process.security.ProcessAiIntentCipher;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.status.ProcessAiDialogueV2Feature;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessAiParseRevisionService {

    private final ProcessAiParseRepository repository;
    private final ProcessAiConfirmationCodec codec;
    private final ProcessAiCorrectionService correctionService;
    private final ProcessAiPlanCompilationService compilationService;
    private final ProcessAiDefaultResolver defaultResolver;
    private final ProcessAiPreviewHashService previewHashService;
    private final ProcessAiIntentCipher intentCipher;
    private final ProcessAiConfirmationContextGuard contextGuard;
    private final ObjectMapper objectMapper;
    private final ProcessAiMessageService messageService;
    private final ProcessTextRedactor redactor;
    private final ProcessAiDialogueV2Feature dialogueV2Feature;

    @Transactional
    public ProcessAiParseResultResponse revise(String orderUuid, ProcessAiReviseRequest request) {
        dialogueV2Feature.requireEnabled(orderUuid);
        ProcessAiParseRecord record = repository.findByParseIdForUpdate(request.parseId())
                .orElseThrow(() -> notFound("AI_PARSE_NOT_FOUND", "AI解析不存在"));
        requireIdentity(record, orderUuid, request);
        if ("UNDERSTANDING".equals(record.resultKind()) || "FAILURE".equals(record.resultKind())) {
            throw conflict("AI_PARSE_NOT_REVISIONABLE", "当前理解结果尚未形成可修正工艺");
        }
        ProcessAiOrderContext context = contextGuard.lockAndRead(orderUuid,
                request.conversationId(), request.expectedVersion(), record.memoryGeneration());
        ProcessAiExtractionResult extraction = codec.readExtraction(record);
        ProcessAiExtractionResult revised = correctionService.apply(
                extraction, request.corrections());
        String original = messageService.restoreUserMessage(
                orderUuid, record.conversationId(), record.expectedVersion(),
                record.requestIdempotencyKey());
        ProcessAiCompilationResult compilation = compilationService.compile(
                revised, context, redactor.redact(original).charges());
        List<ProcessAiDefaultValue> defaults = compilation.eligible()
                ? defaultResolver.resolve(revised, context) : List.of();
        int nextRevision = record.parseRevision() + 1;
        String extractionJson = codec.write(revised);
        String correctionsJson = intentCipher.encrypt(record.conversationId(), nextRevision,
                codec.write(request.corrections()));
        String intentJson = intentCipher.encrypt(record.conversationId(), nextRevision, extractionJson);
        String extractionHash = ProcessAiAuditHasher.sha256(extractionJson);
        String correctionsHash = ProcessAiAuditHasher.sha256(codec.write(request.corrections()));
        String inputHash = ProcessAiAuditHasher.sha256(record.inputHash(), correctionsJson);
        String contextHash = ProcessAiAuditHasher.sha256(orderUuid,
                Integer.toString(request.expectedVersion()), Integer.toString(record.memoryGeneration()),
                record.projectMemoryVersion(), record.projectMemoryChecksum());
        boolean blocked = revised.needsClarification() || !revised.unmappedText().isEmpty()
                || !revised.conflicts().isEmpty() || !revised.clarificationQuestions().isEmpty();
        String status = blocked ? "CLARIFICATION" : compilation.eligible() ? "READY" : "REJECTED";
        String dialogueState = blocked ? "CLARIFYING"
                : compilation.eligible() ? "PREVIEW_READY" : "REVISING";
        String questionJson = revised.clarificationQuestions().isEmpty() ? null
                : intentCipher.encrypt(record.conversationId(), nextRevision,
                codec.write(ProcessAiClarificationQuestionMapper.fromExtraction(
                        revised.clarificationQuestions(), nextRevision)));
        String previewHash = compilation.eligible() && !blocked
                ? previewHash(record, revised, compilation, defaults, extractionHash, correctionsHash)
                : null;
        if (repository.revise(record.parseId(), record.parseRevision(), nextRevision, status,
                dialogueState, intentJson,
                correctionsJson, inputHash, contextHash, previewHash, extractionHash,
                codec.write(defaults.stream().map(ProcessAiDefaultValue::defaultId).toList()),
                questionJson) != 1) {
            throw conflict("AI_PARSE_REVISION_CONFLICT", "AI解析版本已被更新，请重新预览");
        }
        return response(record, revised, compilation, defaults, nextRevision, previewHash, status,
                dialogueState, context.baseline());
    }

    private ProcessAiParseResultResponse response(ProcessAiParseRecord record,
                                                   ProcessAiExtractionResult extraction,
                                                   ProcessAiCompilationResult compilation,
                                                   List<ProcessAiDefaultValue> defaults,
                                                   int revision, String previewHash, String status,
                                                   String dialogueState,
                                                   com.paper.mes.ai.process.context.ProcessAiReviewBaseline baseline) {
        var compiled = new ProcessAiParseResultResponse.CompiledStatus(
                compilation.eligible(), compilation.rollConfigurations(), compilation.plans(), compilation.packagingCandidates(),
                compilation.errors(), compilation.warnings());
        return new ProcessAiParseResultResponse(record.conversationId(), record.parseId(),
                revision, record.expectedVersion(), null, status, baseline, extraction, compiled,
                OffsetDateTime.now().plusMinutes(30), "EXTRACTION",
                dialogueState, null, ProcessAiClarificationQuestionMapper.fromExtraction(
                        extraction.clarificationQuestions(), revision),
                defaults.stream().map(ProcessAiDefaultValue::defaultId).toList(), previewHash);
    }

    private String previewHash(ProcessAiParseRecord record, ProcessAiExtractionResult extraction,
                               ProcessAiCompilationResult compilation,
                               List<ProcessAiDefaultValue> defaults, String extractionHash,
                               String correctionsHash) {
        try {
            return previewHashService.hash(new ProcessAiPreviewHashInput(
                    record.orderUuid(), record.expectedVersion(), record.conversationId(),
                    record.memoryGeneration(), record.projectMemoryVersion(),
                    record.projectMemoryChecksum(), extractionHash,
                    extractionHash, correctionsHash,
                    defaults.stream().map(ProcessAiDefaultValue::defaultId).toList(),
                    objectMapper.readTree(codec.write(compilation.rollConfigurations())),
                    objectMapper.readTree(codec.write(compilation.plans())),
                    objectMapper.readTree(codec.write(compilation.packagingCandidates()))));
        } catch (Exception ex) {
            throw new IllegalStateException("AI preview hash input is invalid", ex);
        }
    }

    private void requireIdentity(ProcessAiParseRecord record, String orderUuid,
                                 ProcessAiReviseRequest request) {
        if (!record.orderUuid().equals(orderUuid)
                || !record.conversationId().equals(request.conversationId())
                || record.expectedVersion() != request.expectedVersion()
                || record.parseRevision() != request.parseRevision()) {
            throw conflict("AI_PARSE_REVISION_CONFLICT", "AI解析版本已过期，请重新预览");
        }
        if (!SetOfRevisionable.contains(record.status())) {
            throw conflict("AI_PARSE_NOT_REVISIONABLE", "当前AI解析状态不允许修正");
        }
    }

    private BusinessException notFound(String code, String message) {
        return new BusinessException(ResultCode.NOT_FOUND, code, message);
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(ResultCode.CONFLICT, code, message);
    }

    private static final java.util.Set<String> SetOfRevisionable =
            java.util.Set.of("READY", "CLARIFICATION", "REJECTED");
}
