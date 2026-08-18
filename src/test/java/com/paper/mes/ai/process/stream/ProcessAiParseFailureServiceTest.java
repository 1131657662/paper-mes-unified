package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.session.ProcessAiConversationService;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseStreamRequest;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiParseFailureServiceTest {

    @Test
    void lateFailureDoesNotInterruptConversationAfterFinalMessageWasStored() {
        ProcessAiConversationService conversationService = mock(ProcessAiConversationService.class);
        ProcessAiMessageService messageService = mock(ProcessAiMessageService.class);
        ProcessAiParseAuditRecorder auditRecorder = mock(ProcessAiParseAuditRecorder.class);
        ProcessAiParseFailureService service = new ProcessAiParseFailureService(
                conversationService, messageService, auditRecorder);
        ProcessAiPreparedParse prepared = mock(ProcessAiPreparedParse.class);
        when(prepared.orderUuid()).thenReturn("order-1");
        when(prepared.assistantSequence()).thenReturn(2);
        when(prepared.request()).thenReturn(new ProcessAiParseStreamRequest(
                7, "conversation-1", "request-1", "START", "cut"));
        doThrow(new BusinessException(ResultCode.CONFLICT,
                "AI_MESSAGE_UPDATE_CONFLICT", "terminal"))
                .when(messageService).updateAssistant(any());

        service.fail(prepared, "AI_PROVIDER_TIMEOUT");

        verify(conversationService, never()).markInterrupted("order-1", "conversation-1");
        verify(auditRecorder).failure(prepared, "AI_PROVIDER_TIMEOUT");
    }
}
