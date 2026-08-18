package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.memory.ProjectMemoryDocumentProvider;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
class ProcessAiParsePreparationService {

    private final CloudDbContextReader contextReader;
    private final ProcessAiConversationService conversationService;
    private final ProcessAiMessageService messageService;
    private final ProjectMemoryDocumentProvider memoryProvider;
    private final ProcessTextRedactor redactor;

    ProcessAiPreparedParse prepare(String orderUuid, ProcessAiParseStreamRequest request) {
        ProcessAiOrderContext order = contextReader.read(orderUuid, request.expectedVersion());
        String original = requirement(request, order);
        ProcessTextRedactionResult redaction = redactor.redact(original);
        appendUser(orderUuid, request, original);
        ProcessAiParseReservation reservation = conversationService.reserveParse(
                new ReserveProcessAiParseCommand(orderUuid, request.conversationId(), request.expectedVersion()));
        ProjectMemorySnapshot memory = memoryProvider.version(reservation.projectMemoryVersion())
                .orElseThrow(() -> unavailable("AI_MEMORY_VERSION_UNAVAILABLE", "会话绑定的项目记忆不可用"));
        var messages = messageService.restore(orderUuid, request.conversationId(), request.expectedVersion());
        int assistantSequence = messageService.createAssistant(new CreateAssistantMessageCommand(
                orderUuid, request.conversationId(), request.expectedVersion())).sequenceNo();
        return new ProcessAiPreparedParse(orderUuid, UUID.randomUUID().toString(), request,
                order, reservation, memory, redaction, messages, assistantSequence,
                System.nanoTime());
    }

    private void appendUser(String orderUuid, ProcessAiParseStreamRequest request, String content) {
        messageService.appendUser(new AppendUserMessageCommand(
                orderUuid, request.conversationId(), request.expectedVersion(),
                request.idempotencyKey(), content));
    }

    private String requirement(ProcessAiParseStreamRequest request, ProcessAiOrderContext order) {
        String message = request.message() == null ? "" : request.message().trim();
        if ("CLARIFY".equals(request.action()) && message.isEmpty()) {
            throw badRequest("AI_CLARIFICATION_EMPTY", "补充说明不能为空");
        }
        if (!message.isEmpty()) return message;
        if ("START".equals(request.action()) && order.remarkLong() != null
                && !order.remarkLong().isBlank()) return order.remarkLong().trim();
        throw badRequest("AI_PROCESS_TEXT_EMPTY", "请先填写或粘贴客户加工要求");
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, code, message);
    }

    private BusinessException unavailable(String code, String message) {
        return new BusinessException(ResultCode.SERVICE_UNAVAILABLE, code, message);
    }
}
