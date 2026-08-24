package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.memory.ProjectMemoryDocumentProvider;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.intent.ProcessAiClarificationQuestion;
import com.paper.mes.ai.process.parse.ProcessAiClarificationValidator;
import com.paper.mes.ai.process.security.ProcessTextRedactionResult;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import com.paper.mes.ai.process.session.ProcessAiConversationService;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.session.dto.AppendUserMessageCommand;
import com.paper.mes.ai.process.session.dto.CreateAssistantMessageCommand;
import com.paper.mes.ai.process.session.dto.ProcessAiParseReservation;
import com.paper.mes.ai.process.session.dto.ReserveProcessAiParseCommand;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseStreamRequest;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class ProcessAiParsePreparationService {

    private final CloudDbContextReader contextReader;
    private final ProcessAiConversationService conversationService;
    private final ProcessAiMessageService messageService;
    private final ProjectMemoryDocumentProvider memoryProvider;
    private final ProcessTextRedactor redactor;
    private final ProcessAiClarificationValidator clarificationValidator;

    @Transactional(rollbackFor = Exception.class)
    public ProcessAiPreparedParse prepare(String orderUuid, ProcessAiParseStreamRequest request) {
        ProcessAiClarificationQuestion clarificationQuestion = validateClarification(orderUuid, request);
        ProcessAiOrderContext order = contextReader.read(orderUuid, request.expectedVersion());
        String original = requirement(request, order);
        ProcessTextRedactionResult redaction = redactor.redact(original);
        ProcessAiParseReservation reservation = conversationService.reserveParse(
                new ReserveProcessAiParseCommand(orderUuid, request.conversationId(),
                        request.expectedVersion(), request.action()));
        ProjectMemorySnapshot memory = memoryProvider.version(reservation.projectMemoryVersion())
                .orElseThrow(() -> unavailable("AI_MEMORY_VERSION_UNAVAILABLE", "会话绑定的项目记忆不可用"));
        appendUser(orderUuid, request, original);
        var messages = messageService.restore(orderUuid, request.conversationId(), request.expectedVersion());
        int assistantSequence = messageService.createAssistant(new CreateAssistantMessageCommand(
                orderUuid, request.conversationId(), request.expectedVersion())).sequenceNo();
        return new ProcessAiPreparedParse(orderUuid, UUID.randomUUID().toString(), request,
                order, reservation, memory, redaction, messages, clarificationQuestion,
                assistantSequence, System.nanoTime());
    }

    private ProcessAiClarificationQuestion validateClarification(String orderUuid,
                                                                  ProcessAiParseStreamRequest request) {
        if (!"CLARIFY".equals(request.action())) return null;
        if (request.questionId() == null || request.parseId() == null
                || request.parseRevision() == null) {
            throw badRequest("AI_CLARIFICATION_CONTEXT_REQUIRED", "澄清回答缺少当前问题版本");
        }
        return clarificationValidator.validate(orderUuid, request.conversationId(),
                request.expectedVersion(), request.parseId(), request.parseRevision(),
                request.questionId(), request.answerCode(), request.answerText());
    }

    private void appendUser(String orderUuid, ProcessAiParseStreamRequest request, String content) {
        messageService.appendUser(new AppendUserMessageCommand(
                orderUuid, request.conversationId(), request.expectedVersion(),
                request.idempotencyKey(), content));
    }

    private String requirement(ProcessAiParseStreamRequest request, ProcessAiOrderContext order) {
        if ("CLARIFY".equals(request.action()) && request.parseId() != null) {
            return structuredAnswer(request);
        }
        String message = request.message() == null ? "" : request.message().trim();
        if ("CLARIFY".equals(request.action()) && message.isEmpty()) {
            message = request.answerText() == null ? "" : request.answerText().trim();
            if (message.isEmpty() && request.answerCode() != null) message = request.answerCode();
            if (message.isEmpty()) throw badRequest("AI_CLARIFICATION_EMPTY", "补充说明不能为空");
        }
        if (!message.isEmpty()) return message;
        if ("START".equals(request.action()) && order.remarkLong() != null
                && !order.remarkLong().isBlank()) return order.remarkLong().trim();
        throw badRequest("AI_PROCESS_TEXT_EMPTY", "请先填写或粘贴客户加工要求");
    }

    private String structuredAnswer(ProcessAiParseStreamRequest request) {
        String answerCode = request.answerCode() == null ? "" : request.answerCode().trim();
        String answerText = request.answerText() == null ? "" : request.answerText().trim();
        if (!answerCode.isEmpty() && !answerText.isEmpty()) return answerCode + "：" + answerText;
        if (!answerCode.isEmpty()) return answerCode;
        if (!answerText.isEmpty()) return answerText;
        throw badRequest("AI_CLARIFICATION_EMPTY", "补充说明不能为空");
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, code, message);
    }

    private BusinessException unavailable(String code, String message) {
        return new BusinessException(ResultCode.SERVICE_UNAVAILABLE, code, message);
    }
}
