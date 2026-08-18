package com.paper.mes.ai.process.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.parse.ProcessAiParseRecord;
import com.paper.mes.ai.process.security.ProcessAiIntentCipher;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;

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
        int count = execution.extraction().assignments().size();
        if ("CLARIFICATION".equals(status)) {
            String questions = String.join("；", execution.extraction().clarificationQuestions());
            return "AI 已识别 " + count + " 组工艺，但仍需要补充信息。"
                    + (questions.isBlank() ? "" : "\n" + questions);
        }
        if ("REJECTED".equals(status)) {
            return "AI 已识别 " + count + " 组工艺，但现有工艺预览未通过。";
        }
        return "AI 已识别 " + count + " 组工艺，请核对解析依据和字段变化。";
    }

    ProcessAiParseResultResponse completed(ProcessAiPreparedParse prepared,
                                            ProcessAiModelExecution execution,
                                            String status,
                                            ProcessAiCompilationResult compilation) {
        return new ProcessAiParseResultResponse(
                prepared.request().conversationId(), prepared.parseId(),
                prepared.reservation().parseRevision(), prepared.request().expectedVersion(),
                null, status, prepared.orderContext().baseline(),
                execution.extraction(), compiled(compilation),
                OffsetDateTime.now().plusMinutes(30));
    }

    ProcessAiParseResultResponse replayed(ProcessAiParseRecord record,
                                           ProcessAiExtractionResult extraction,
                                           ProcessAiCompilationResult compilation,
                                           ProcessAiOrderContext context) {
        OffsetDateTime expires = record.createdAt().atZone(ZoneId.systemDefault())
                .toOffsetDateTime().plusMinutes(30);
        return new ProcessAiParseResultResponse(
                record.conversationId(), record.parseId(), record.parseRevision(),
                record.expectedVersion(), null, status(extraction, compilation),
                context.baseline(), extraction, compiled(compilation), expires);
    }

    private ProcessAiParseResultResponse.CompiledStatus compiled(
            ProcessAiCompilationResult compilation) {
        return new ProcessAiParseResultResponse.CompiledStatus(
                compilation.eligible(), compilation.plans(),
                compilation.packagingCandidates(),
                compilation.errors(), compilation.warnings());
    }
}
