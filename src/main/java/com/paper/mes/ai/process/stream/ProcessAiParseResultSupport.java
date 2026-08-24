package com.paper.mes.ai.process.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiClarificationQuestionMapper;
import com.paper.mes.ai.process.intent.ProcessAiUnderstandingResult;
import com.paper.mes.ai.process.parse.ProcessAiParseRecord;
import com.paper.mes.ai.process.security.ProcessAiIntentCipher;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
class ProcessAiParseResultSupport {

    private final ObjectMapper objectMapper;
    private final ProcessAiIntentCipher intentCipher;

    ProcessAiExtractionResult readExtraction(ProcessAiParseRecord record) {
        try {
            String json = intentCipher.decrypt(
                    record.conversationId(), record.parseRevision(), record.intentJson());
            return objectMapper.readValue(json, ProcessAiExtractionResult.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("stored AI parse result is invalid", ex);
        }
    }

    ProcessAiUnderstandingResult readUnderstanding(ProcessAiParseRecord record) {
        try {
            String json = intentCipher.decrypt(
                    record.conversationId(), record.parseRevision(), record.understandingJson());
            return objectMapper.readValue(json, ProcessAiUnderstandingResult.class);
        } catch (Exception ex) {
            throw new IllegalStateException("stored AI understanding is invalid", ex);
        }
    }

    String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("validated AI extraction serialization failed", ex);
        }
    }

    String status(ProcessAiExtractionResult result, ProcessAiCompilationResult compilation) {
        boolean blocked = result.needsClarification() || !result.unmappedText().isEmpty()
                || !result.conflicts().isEmpty() || !result.clarificationQuestions().isEmpty();
        if (blocked) return "CLARIFICATION";
        return compilation.eligible() ? "READY" : "REJECTED";
    }

    String summary(ProcessAiModelExecution execution, String status) {
        if (execution.isUnderstanding()) {
            return "AI 已理解当前要求，但有信息需要你确认后才能生成工艺预览。";
        }
        long ancillaryCount = execution.extraction().assignments().stream()
                .filter(assignment -> "SERVICE_ONLY".equals(assignment.processType())
                        || "ANCILLARY_ONLY".equals(assignment.processType()))
                .count();
        int count = execution.extraction().assignments().size();
        if ("CLARIFICATION".equals(status)) {
            String questions = String.join("；", execution.extraction().clarificationQuestions());
            return "AI 已识别 " + count + " 组工艺，但仍需要补充信息。"
                    + (questions.isBlank() ? "" : "\n" + questions);
        }
        if ("REJECTED".equals(status)) {
            return "AI 已识别 " + count + " 组工艺，但现有工艺预览未通过。";
        }
        if (ancillaryCount == count && count > 0) {
            return "AI 已识别 " + count + " 条附加工艺，请确认后写入当前草稿。";
        }
        return "AI 已识别 " + count + " 组工艺，请核对解析依据和字段变化。";
    }

    ProcessAiParseResultResponse completed(ProcessAiPreparedParse prepared,
                                            ProcessAiModelExecution execution,
                                            String status,
                                            ProcessAiCompilationResult compilation) {
        return completed(prepared, execution, status, compilation, null, List.of());
    }

    ProcessAiParseResultResponse completed(ProcessAiPreparedParse prepared,
                                            ProcessAiModelExecution execution,
                                            String status,
                                            ProcessAiCompilationResult compilation,
                                            String previewHash,
                                            List<String> requiredDefaultIds) {
        return new ProcessAiParseResultResponse(
                prepared.request().conversationId(), prepared.parseId(),
                prepared.reservation().parseRevision(), prepared.request().expectedVersion(),
                null, status, prepared.orderContext().baseline(),
                execution.extraction(), compiled(compilation),
                OffsetDateTime.now().plusMinutes(30), "EXTRACTION",
                "READY".equals(status) ? "PREVIEW_READY" : "CLARIFYING", null,
                ProcessAiClarificationQuestionMapper.fromExtraction(
                        execution.extraction().clarificationQuestions(),
                        prepared.reservation().parseRevision()), requiredDefaultIds, previewHash);
    }

    ProcessAiParseResultResponse understood(ProcessAiPreparedParse prepared,
                                             ProcessAiModelExecution execution) {
        return new ProcessAiParseResultResponse(
                prepared.request().conversationId(), prepared.parseId(),
                prepared.reservation().parseRevision(), prepared.request().expectedVersion(),
                null, "CLARIFICATION", prepared.orderContext().baseline(), null,
                new ProcessAiParseResultResponse.CompiledStatus(
                        false, List.of(), List.of(), List.of("AI需要确认后才能生成工艺预览"),
                        execution.understanding().risks()),
                OffsetDateTime.now().plusMinutes(30), "UNDERSTANDING", "CLARIFYING",
                execution.understanding(), execution.understanding().clarificationQuestions(),
                List.of(), null);
    }

    ProcessAiParseResultResponse replayed(ProcessAiParseRecord record,
                                           ProcessAiExtractionResult extraction,
                                           ProcessAiCompilationResult compilation,
                                           ProcessAiOrderContext context) {
        OffsetDateTime expires = record.createdAt().atZone(ZoneId.systemDefault())
                .toOffsetDateTime().plusMinutes(30);
        return new ProcessAiParseResultResponse(
                record.conversationId(), record.parseId(), record.parseRevision(),
                record.expectedVersion(), null, record.status(),
                context.baseline(), extraction, compiled(compilation), expires,
                record.resultKind(), record.dialogueState(), null,
                ProcessAiClarificationQuestionMapper.fromExtraction(
                        extraction.clarificationQuestions(), record.parseRevision()),
                parseList(record.requiredDefaultIds()), record.previewHash());
    }

    ProcessAiParseResultResponse replayUnderstanding(ProcessAiParseRecord record,
                                                      ProcessAiUnderstandingResult understanding,
                                                      ProcessAiOrderContext context) {
        OffsetDateTime expires = record.createdAt().atZone(ZoneId.systemDefault())
                .toOffsetDateTime().plusMinutes(30);
        return new ProcessAiParseResultResponse(record.conversationId(), record.parseId(),
                record.parseRevision(), record.expectedVersion(), null, "CLARIFICATION",
                context.baseline(), null,
                new ProcessAiParseResultResponse.CompiledStatus(false, List.of(), List.of(),
                        List.of("AI需要确认后才能生成工艺预览"), understanding.risks()), expires,
                "UNDERSTANDING", record.dialogueState(), understanding,
                understanding.clarificationQuestions(), parseList(record.requiredDefaultIds()),
                record.previewHash());
    }

    ProcessAiParseResultResponse replayFailure(ProcessAiParseRecord record,
                                               ProcessAiOrderContext context) {
        OffsetDateTime expires = record.createdAt().atZone(ZoneId.systemDefault())
                .toOffsetDateTime().plusMinutes(30);
        return new ProcessAiParseResultResponse(record.conversationId(), record.parseId(),
                record.parseRevision(), record.expectedVersion(), null, "INTERRUPTED",
                context.baseline(), null,
                new ProcessAiParseResultResponse.CompiledStatus(false, List.of(), List.of(),
                        List.of("AI解析未完成，请使用新的请求标识重试"), List.of()), expires,
                "FAILURE", "FAILED", null, List.of(), List.of(), null);
    }

    private List<String> parseList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception ex) {
            throw new IllegalStateException("stored AI defaults are invalid", ex);
        }
    }

    private ProcessAiParseResultResponse.CompiledStatus compiled(
            ProcessAiCompilationResult compilation) {
        return new ProcessAiParseResultResponse.CompiledStatus(
                compilation.eligible(), compilation.rollConfigurations(), compilation.plans(),
                compilation.packagingCandidates(),
                compilation.errors(), compilation.warnings());
    }
}
