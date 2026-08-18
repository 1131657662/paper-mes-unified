package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.session.ProcessAiConversationService;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.session.dto.UpdateAssistantMessageCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class ProcessAiParseFailureService {

    private final ProcessAiConversationService conversationService;
    private final ProcessAiMessageService messageService;
    private final ProcessAiParseAuditRecorder auditRecorder;

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
        if (persistFailedMessage(prepared, failureCode)) {
            conversationService.markInterrupted(
                    prepared.orderUuid(), prepared.request().conversationId());
        }
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
}
