package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.process.parse.ProcessAiFailureStoreCommand;
import com.paper.mes.ai.process.parse.ProcessAiParseStoreService;
import com.paper.mes.ai.process.session.ProcessAiConversationService;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.session.dto.UpdateAssistantMessageCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
class ProcessAiParseFailureService {

    private final ProcessAiConversationService conversationService;
    private final ProcessAiMessageService messageService;
    private final ProcessAiParseAuditRecorder auditRecorder;
    private final ProcessAiParseStoreService parseStore;
    private final AiProperties properties;

    ProcessAiParseFailureService(ProcessAiConversationService conversationService,
                                 ProcessAiMessageService messageService,
                                 ProcessAiParseAuditRecorder auditRecorder) {
        this(conversationService, messageService, auditRecorder, null, null);
    }

    @Autowired
    ProcessAiParseFailureService(ProcessAiConversationService conversationService,
                                 ProcessAiMessageService messageService,
                                 ProcessAiParseAuditRecorder auditRecorder,
                                 ProcessAiParseStoreService parseStore,
                                 AiProperties properties) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.auditRecorder = auditRecorder;
        this.parseStore = parseStore;
        this.properties = properties;
    }

    void fail(ProcessAiPreparedParse prepared, String failureCode) {
        try {
            persistInterrupted(prepared, failureCode);
        } catch (RuntimeException ex) {
            log.error("Could not persist interrupted AI parse state: conversationId={}, type={}",
                    prepared.request().conversationId(), ex.getClass().getSimpleName());
        }
        try {
            auditRecorder.failure(prepared, failureCode);
        } catch (RuntimeException ex) {
            log.error("Could not persist failed AI call audit: conversationId={}, type={}",
                    prepared.request().conversationId(), ex.getClass().getSimpleName());
        }
    }

    void failBeforeStart(ProcessAiPreparedParse prepared, String failureCode) {
        persistFailureRecord(prepared, failureCode);
        try {
            persistFailedMessage(prepared, failureCode);
        } catch (RuntimeException ex) {
            log.error("Could not persist rejected AI parse state: conversationId={}, type={}",
                    prepared.request().conversationId(), ex.getClass().getSimpleName());
        }
        try {
            auditRecorder.failure(prepared, failureCode);
        } catch (RuntimeException ex) {
            log.error("Could not persist rejected AI call audit: conversationId={}, type={}",
                    prepared.request().conversationId(), ex.getClass().getSimpleName());
        }
    }

    private void persistInterrupted(ProcessAiPreparedParse prepared, String failureCode) {
        persistFailureRecord(prepared, failureCode);
        if (persistFailedMessage(prepared, failureCode)) {
            conversationService.markInterrupted(
                    prepared.orderUuid(), prepared.request().conversationId());
        }
    }

    private void persistFailureRecord(ProcessAiPreparedParse prepared, String failureCode) {
        if (parseStore == null || properties == null) return;
        try {
            parseStore.storeFailure(new ProcessAiFailureStoreCommand(
                    prepared.orderUuid(), prepared.request().conversationId(), prepared.parseId(),
                    prepared.request().expectedVersion(), prepared.reservation().parseRevision(),
                    prepared.reservation().memoryGeneration(), prepared.request().idempotencyKey(),
                    prepared.memory(), properties.effectiveProvider().name(), model(),
                    "PRIMARY", failureCode, java.util.UUID.randomUUID().toString()));
        } catch (RuntimeException ex) {
            log.error("Could not persist AI failure result: conversationId={}, parseId={}, type={}, causeType={}, causeMessage={}",
                    prepared.request().conversationId(), prepared.parseId(),
                    ex.getClass().getSimpleName(), causeType(ex), causeMessage(ex));
        }
    }

    private String model() {
        return properties.effectiveProvider() == com.paper.mes.ai.config.AiProvider.ZHIPU
                ? properties.getZhipuModel() : properties.getDeepseekModelPro();
    }

    private boolean persistFailedMessage(ProcessAiPreparedParse prepared, String failureCode) {
        String interruption = "AI 解析中断，请使用相同要求重试。错误代码：" + failureCode;
        try {
            messageService.updateAssistant(new UpdateAssistantMessageCommand(
                    prepared.orderUuid(), prepared.request().conversationId(),
                    prepared.request().expectedVersion(), prepared.assistantSequence(),
                    interruption,
                    "FAILED", null));
            return true;
        } catch (RuntimeException exception) {
            if (exception instanceof com.paper.mes.common.BusinessException business
                    && "AI_MESSAGE_UPDATE_CONFLICT".equals(business.getErrorCode())) {
                log.info("Skipping late AI failure after terminal assistant message: conversationId={}",
                        prepared.request().conversationId());
                return false;
            }
            throw exception;
        }
    }

    private String causeType(RuntimeException exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        return cause.getClass().getSimpleName();
    }

    private String causeMessage(RuntimeException exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) return "<none>";
        message = message.replaceAll("[\\r\\n]+", " ");
        return message.length() > 240 ? message.substring(0, 240) : message;
    }
}
