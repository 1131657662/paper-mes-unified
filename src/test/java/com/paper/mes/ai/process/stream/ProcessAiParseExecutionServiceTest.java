package com.paper.mes.ai.process.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiExtractionParser;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiGroupedPiecePlanGuard;
import com.paper.mes.ai.process.intent.ProcessAiIntentNormalizer;
import com.paper.mes.ai.process.intent.ProcessAiIntentValidator;
import com.paper.mes.ai.process.intent.ProcessAiSawRemainderResolver;
import com.paper.mes.ai.process.intent.ProcessAiSourceAssignmentResolver;
import com.paper.mes.ai.process.model.ProcessAiModelRetryExecutor;
import com.paper.mes.ai.process.model.ProcessAiCancellation;
import com.paper.mes.ai.process.model.ProcessAiModelResult;
import com.paper.mes.ai.process.model.ProcessAiProviderException;
import com.paper.mes.ai.process.prompt.ProcessAiPromptAssembler;
import com.paper.mes.ai.process.prompt.ProcessAiPromptBundle;
import com.paper.mes.ai.process.model.ProcessAiModelPrompt;
import com.paper.mes.ai.process.security.ProcessTextRedactionResult;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.session.dto.ProcessAiParseReservation;
import com.paper.mes.ai.process.session.dto.UpdateAssistantMessageCommand;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseStreamRequest;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;
import org.springframework.dao.TransientDataAccessResourceException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiParseExecutionServiceTest {

    @Test
    void executeDoesNotEchoUnvalidatedProviderContentWhenProviderFailsAfterStreaming() {
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

        org.mockito.Mockito.verify(messageService, org.mockito.Mockito.never())
                .updateAssistant(org.mockito.ArgumentMatchers.any(
                        com.paper.mes.ai.process.session.dto.UpdateAssistantMessageCommand.class));
    }

    @Test
    void executeUsesFallbackRouteAfterInvalidContractWithoutReinvokingPrimary() {
        ProcessAiPromptAssembler promptAssembler = mock(ProcessAiPromptAssembler.class);
        ProcessAiModelRetryExecutor modelExecutor = mock(ProcessAiModelRetryExecutor.class);
        ProcessAiExtractionParser extractionParser = mock(ProcessAiExtractionParser.class);
        ProcessAiMessageService messageService = mock(ProcessAiMessageService.class);
        ProcessAiParseExecutionService service = new ProcessAiParseExecutionService(
                promptAssembler, modelExecutor, extractionParser,
                new ProcessAiIntentNormalizer(), mock(ProcessAiSourceAssignmentResolver.class),
                mock(ProcessAiGroupedPiecePlanGuard.class), mock(ProcessAiSawRemainderResolver.class),
                mock(ProcessAiIntentValidator.class), mock(ProcessAiParseCompletionService.class),
                messageService);
        ProcessAiPromptBundle initial = new ProcessAiPromptBundle(
                new ProcessAiModelPrompt("system", "initial"), List.of());
        ProcessAiPromptBundle retry = new ProcessAiPromptBundle(
                new ProcessAiModelPrompt("system", "contract-retry"), List.of());
        when(promptAssembler.assemble(any())).thenReturn(initial);
        when(promptAssembler.assembleContractRetry(any(), eq("AI_MODEL_RESULT_INVALID")))
                .thenReturn(retry);
        when(modelExecutor.parse(any(), any(), any()))
                .thenReturn(new com.paper.mes.ai.process.model.ProcessAiModelResult(
                        "primary-invalid", "deepseek", "DEEPSEEK", "PRO", 1, 1));
        when(modelExecutor.parseFallback(any(), any(), any()))
                .thenReturn(new com.paper.mes.ai.process.model.ProcessAiModelResult(
                        "fallback-invalid", "glm", "ZHIPU", "PRO", 1, 1));
        ProcessAiProviderException invalid = new ProcessAiProviderException(
                "AI_MODEL_RESULT_INVALID", false, "invalid contract");
        when(extractionParser.parse(any())).thenThrow(invalid);

        assertThatThrownBy(() -> service.execute(
                prepared(), mock(ProcessAiStreamSink.class), new ProcessAiCancellation()))
                .isSameAs(invalid);

        verify(modelExecutor, times(1)).parse(any(), any(), any());
        verify(modelExecutor, times(1)).parseFallback(any(), any(), any());
    }

    @Test
    void executeRetriesCompletionWithoutReinvokingTheModel() {
        ProcessAiPromptAssembler promptAssembler = mock(ProcessAiPromptAssembler.class);
        ProcessAiModelRetryExecutor modelExecutor = mock(ProcessAiModelRetryExecutor.class);
        ProcessAiExtractionParser extractionParser = mock(ProcessAiExtractionParser.class);
        ProcessAiSourceAssignmentResolver sourceResolver = mock(ProcessAiSourceAssignmentResolver.class);
        ProcessAiGroupedPiecePlanGuard groupedGuard = mock(ProcessAiGroupedPiecePlanGuard.class);
        ProcessAiSawRemainderResolver remainderResolver = mock(ProcessAiSawRemainderResolver.class);
        ProcessAiIntentValidator validator = mock(ProcessAiIntentValidator.class);
        ProcessAiParseCompletionService completion = mock(ProcessAiParseCompletionService.class);
        ProcessAiMessageService messageService = mock(ProcessAiMessageService.class);
        ProcessAiParseResultResponse response = mock(ProcessAiParseResultResponse.class);
        ProcessAiExtractionResult extraction = extraction();
        ProcessAiModelResult model = new ProcessAiModelResult("{}", "deepseek", "DEEPSEEK", "PRO", 1, 1);
        ProcessAiParseExecutionService service = new ProcessAiParseExecutionService(
                promptAssembler, modelExecutor, extractionParser, new ProcessAiIntentNormalizer(),
                sourceResolver, groupedGuard, remainderResolver, validator, completion, messageService);
        when(promptAssembler.assemble(any())).thenReturn(new ProcessAiPromptBundle(
                new ProcessAiModelPrompt("system", "context"), List.of()));
        when(modelExecutor.parse(any(), any(), any())).thenReturn(model);
        when(extractionParser.parse(any())).thenReturn(extraction);
        when(sourceResolver.resolve(any(), any(), any())).thenReturn(extraction);
        when(groupedGuard.resolve(any(), any())).thenReturn(extraction);
        when(remainderResolver.resolve(any(), any(), any())).thenReturn(extraction);
        when(completion.complete(any(), any()))
                .thenThrow(new TransientDataAccessResourceException("connection reset"))
                .thenReturn(response);

        service.execute(prepared(), mock(ProcessAiStreamSink.class), new ProcessAiCancellation());

        verify(modelExecutor, times(1)).parse(any(), any(), any());
        verify(completion, times(2)).complete(any(), any());
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
                List.of(), null, 3, System.nanoTime());
    }

    private ProcessAiExtractionResult extraction() {
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW", null,
                new com.paper.mes.ai.process.intent.ProcessAiSawIntent("CUTS", 1, null, "mm"),
                null, List.of(new com.paper.mes.ai.process.intent.ProcessAiEvidence(
                        "sawIntent", "切一刀")));
        return new ProcessAiExtractionResult("parse-1", "1.0", List.of(assignment),
                List.of(), List.of(), false, List.of());
    }
}
