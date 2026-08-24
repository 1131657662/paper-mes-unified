package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.memory.ProjectMemoryDocumentProvider;
import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.parse.ProcessAiClarificationValidator;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import com.paper.mes.ai.process.session.ProcessAiConversationService;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.session.dto.ProcessAiParseReservation;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseStreamRequest;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiParsePreparationServiceTest {

    @Test
    void unavailableBoundMemoryDoesNotAppendAnOrphanUserMessage() {
        CloudDbContextReader contextReader = mock(CloudDbContextReader.class);
        ProcessAiConversationService conversationService = mock(ProcessAiConversationService.class);
        ProcessAiMessageService messageService = mock(ProcessAiMessageService.class);
        ProjectMemoryDocumentProvider memoryProvider = mock(ProjectMemoryDocumentProvider.class);
        ProcessAiParsePreparationService service = new ProcessAiParsePreparationService(
                contextReader, conversationService, messageService, memoryProvider,
                new ProcessTextRedactor(), mock(ProcessAiClarificationValidator.class));
        ProcessAiParseStreamRequest request = new ProcessAiParseStreamRequest(
                7, "conversation-1", "request-1", "START", "复卷800mm");
        when(contextReader.read("order-1", 7)).thenReturn(
                new ProcessAiOrderContext("order-1", 7, "", List.of()));
        when(conversationService.reserveParse(any())).thenReturn(
                new ProcessAiParseReservation("conversation-1", 1, "memory-1", 1));
        when(memoryProvider.version("memory-1")).thenReturn(Optional.empty());

        BusinessException error = catchThrowableOfType(
                () -> service.prepare("order-1", request), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_MEMORY_VERSION_UNAVAILABLE");
        verify(messageService, never()).appendUser(any());
    }
}
