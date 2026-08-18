package com.paper.mes.ai.process.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.intent.ProcessAiExtractionParser;
import com.paper.mes.ai.process.intent.ProcessAiGroupedPiecePlanGuard;
import com.paper.mes.ai.process.intent.ProcessAiIntentNormalizer;
import com.paper.mes.ai.process.intent.ProcessAiIntentValidator;
import com.paper.mes.ai.process.intent.ProcessAiSawRemainderResolver;
import com.paper.mes.ai.process.intent.ProcessAiSourceAssignmentResolver;
import com.paper.mes.ai.process.model.ProcessAiModelRetryExecutor;
import com.paper.mes.ai.process.model.ProcessAiCancellation;
import com.paper.mes.ai.process.model.ProcessAiProviderException;
import com.paper.mes.ai.process.prompt.ProcessAiPromptAssembler;
import com.paper.mes.ai.process.prompt.ProcessAiPromptBundle;
import com.paper.mes.ai.process.model.ProcessAiModelPrompt;
import com.paper.mes.ai.process.security.ProcessTextRedactionResult;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.session.dto.ProcessAiParseReservation;
import com.paper.mes.ai.process.session.dto.UpdateAssistantMessageCommand;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseStreamRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiParseExecutionServiceTest {

    @Test
    void executePersistsPartialAssistantContentWhenProviderFailsAfterStreaming() {
        ProcessAiPromptAssembler promptAssembler = mock(ProcessAiPromptAssembler.class);
        ProcessAiModelRetryExecutor modelExecutor = mock(ProcessAiModelRetryExecutor.class);
        ProcessAiMessageService messageService = mock(ProcessAiMessageService.class);
        ProcessAiParseExecutionService service = service(
                promptAssembler, modelExecutor, messageService);
        when(promptAssembler.assemble(any())).thenReturn(new ProcessAiPromptBundle(
                new ProcessAiModelPrompt("system", "context"), List.of()));
        ProcessAiProviderException failure = new ProcessAiProviderException(
                "AI_PROVIDER_TIMEOUT", true, "timeout");
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> consumer = invocation.getArgument(1);
            consumer.accept("partial response");
            throw failure;
        }).when(modelExecutor).parse(any(), any(), any());

        assertThatThrownBy(() -> service.execute(
                prepared(), mock(ProcessAiStreamSink.class), new ProcessAiCancellation()))
                .isSameAs(failure);

        ArgumentCaptor<UpdateAssistantMessageCommand> captor =
                ArgumentCaptor.forClass(UpdateAssistantMessageCommand.class);
        verify(messageService).updateAssistant(captor.capture());
        assertThat(captor.getValue().content()).isEqualTo("partial response");
        assertThat(captor.getValue().status()).isEqualTo("PARTIAL");
    }

    private ProcessAiParseExecutionService service(ProcessAiPromptAssembler promptAssembler,
                                                   ProcessAiModelRetryExecutor modelExecutor,
                                                   ProcessAiMessageService messageService) {
        return new ProcessAiParseExecutionService(
                promptAssembler, modelExecutor, mock(ProcessAiExtractionParser.class),
                new ProcessAiIntentNormalizer(),
                mock(ProcessAiSourceAssignmentResolver.class),
                mock(ProcessAiGroupedPiecePlanGuard.class),
                mock(ProcessAiSawRemainderResolver.class),
                mock(ProcessAiIntentValidator.class),
                mock(ProcessAiParseCompletionService.class), messageService);
    }

    private ProcessAiPreparedParse prepared() {
        ObjectMapper mapper = new ObjectMapper();
        ProjectMemorySnapshot memory = new ProjectMemorySnapshot(
                "1.0.0", "1.0", "sha256:" + "a".repeat(64),
                mapper.createObjectNode(), Instant.now());
        ProcessAiParseStreamRequest request = new ProcessAiParseStreamRequest(
                7, "conversation-1", "request-1", "START", "cut twice");
        return new ProcessAiPreparedParse(
                "order-1", "parse-1", request,
                new ProcessAiOrderContext("order-1", 7, "cut twice", List.of()),
                new ProcessAiParseReservation("conversation-1", 2, "1.0.0", 1), memory,
                new ProcessTextRedactionResult("cut twice", List.of(), false),
                List.of(), 3, System.nanoTime());
    }
}
