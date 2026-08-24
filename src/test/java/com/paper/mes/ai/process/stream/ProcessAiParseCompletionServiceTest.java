package com.paper.mes.ai.process.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.compile.ProcessAiDefaultResolver;
import com.paper.mes.ai.process.compile.ProcessAiPreviewHashService;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.parse.ProcessAiClarificationValidator;
import com.paper.mes.ai.process.parse.ProcessAiParseRecord;
import com.paper.mes.ai.process.parse.ProcessAiParseStoreCommand;
import com.paper.mes.ai.process.parse.ProcessAiParseStoreService;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.session.dto.ProcessAiParseReservation;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseStreamRequest;
import com.paper.mes.ai.process.model.ProcessAiModelPrompt;
import com.paper.mes.ai.process.prompt.ProcessAiPromptBundle;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiParseCompletionServiceTest {

    @Test
    void complete_unknownAnswerWithoutModelQuestionAddsSafeClarification() {
        ProcessAiParseStoreService store = mock(ProcessAiParseStoreService.class);
        ProcessAiParseResultSupport results = mock(ProcessAiParseResultSupport.class);
        ProcessAiClarificationValidator validator = mock(ProcessAiClarificationValidator.class);
        when(validator.isUnknown("UNKNOWN")).thenReturn(true);
        when(results.status(any(), any())).thenReturn("CLARIFICATION");
        when(results.completed(any(), any(), eq("CLARIFICATION"), any(), any(), any()))
                .thenReturn(mock(ProcessAiParseResultResponse.class));
        when(results.summary(any(), eq("CLARIFICATION"))).thenReturn("需要补充信息");
        when(store.store(any())).thenReturn(mock(ProcessAiParseRecord.class));
        ProcessAiParseCompletionService service = new ProcessAiParseCompletionService(
                store, mock(ProcessAiMessageService.class),
                mock(com.paper.mes.ai.process.compile.ProcessAiPlanCompilationService.class),
                results, mock(ProcessAiParseAuditRecorder.class), mock(ProcessAiDefaultResolver.class),
                mock(ProcessAiPreviewHashService.class), new ObjectMapper(), validator);

        service.complete(prepared(), execution());

        ArgumentCaptor<ProcessAiModelExecution> execution =
                ArgumentCaptor.forClass(ProcessAiModelExecution.class);
        verify(results).completed(any(), execution.capture(), eq("CLARIFICATION"), any(), any(), any());
        assertThat(execution.getValue().extraction().clarificationQuestions()).singleElement()
                .satisfies(question -> assertThat(question).contains("用户暂不确定"));
    }

    @Test
    void complete_blockedExtractionWithoutQuestionAddsFallbackQuestion() {
        ProcessAiParseStoreService store = mock(ProcessAiParseStoreService.class);
        ProcessAiParseResultSupport results = mock(ProcessAiParseResultSupport.class);
        ProcessAiClarificationValidator validator = mock(ProcessAiClarificationValidator.class);
        when(validator.isUnknown(null)).thenReturn(false);
        when(results.status(any(), any())).thenReturn("CLARIFICATION");
        when(results.completed(any(), any(), eq("CLARIFICATION"), any(), any(), any()))
                .thenReturn(mock(ProcessAiParseResultResponse.class));
        when(results.summary(any(), eq("CLARIFICATION"))).thenReturn("需要补充信息");
        when(store.store(any())).thenReturn(mock(ProcessAiParseRecord.class));
        ProcessAiParseCompletionService service = new ProcessAiParseCompletionService(
                store, mock(ProcessAiMessageService.class),
                mock(com.paper.mes.ai.process.compile.ProcessAiPlanCompilationService.class),
                results, mock(ProcessAiParseAuditRecorder.class), mock(ProcessAiDefaultResolver.class),
                mock(ProcessAiPreviewHashService.class), new ObjectMapper(), validator);

        service.complete(prepared(), blockedExecution());

        ArgumentCaptor<ProcessAiModelExecution> execution =
                ArgumentCaptor.forClass(ProcessAiModelExecution.class);
        verify(results).completed(any(), execution.capture(), eq("CLARIFICATION"), any(), any(), any());
        assertThat(execution.getValue().extraction().clarificationQuestions()).singleElement()
                .satisfies(question -> assertThat(question).contains("无法由服务端核验"));
    }

    @Test
    void complete_rejectedPreviewStoresCompletedDialogueState() {
        ProcessAiParseStoreService store = mock(ProcessAiParseStoreService.class);
        ProcessAiParseResultSupport results = mock(ProcessAiParseResultSupport.class);
        ProcessAiClarificationValidator validator = mock(ProcessAiClarificationValidator.class);
        var compiler = mock(com.paper.mes.ai.process.compile.ProcessAiPlanCompilationService.class);
        when(validator.isUnknown(null)).thenReturn(false);
        when(results.status(any(), any())).thenReturn("REJECTED");
        when(results.completed(any(), any(), eq("REJECTED"), any(), any(), any()))
                .thenReturn(mock(ProcessAiParseResultResponse.class));
        when(results.summary(any(), eq("REJECTED"))).thenReturn("预览未通过");
        when(store.store(any())).thenReturn(mock(ProcessAiParseRecord.class));
        when(compiler.compile(any(), any(), any())).thenReturn(
                new com.paper.mes.ai.process.compile.ProcessAiCompilationResult(
                        false, List.of(), List.of(), List.of("blocked"), List.of()));
        ProcessAiParseCompletionService service = new ProcessAiParseCompletionService(
                store, mock(ProcessAiMessageService.class), compiler, results,
                mock(ProcessAiParseAuditRecorder.class), mock(ProcessAiDefaultResolver.class),
                mock(ProcessAiPreviewHashService.class), new ObjectMapper(), validator);

        service.complete(prepared(), execution());

        ArgumentCaptor<ProcessAiParseStoreCommand> command =
                ArgumentCaptor.forClass(ProcessAiParseStoreCommand.class);
        verify(store).store(command.capture());
        assertThat(command.getValue().dialogueState()).isEqualTo("COMPLETED");
    }

    private ProcessAiPreparedParse prepared() {
        ObjectMapper mapper = new ObjectMapper();
        ProjectMemorySnapshot memory = new ProjectMemorySnapshot(
                "1.0.0", "1.0", "sha256:" + "a".repeat(64), mapper.createObjectNode(), Instant.now());
        ProcessAiParseStreamRequest request = new ProcessAiParseStreamRequest(
                7, "conversation-1", "request-1", "CLARIFY", null,
                "question-1", "parse-1", "UNKNOWN", null, 1);
        return new ProcessAiPreparedParse("order-1", "parse-1", request,
                new ProcessAiOrderContext("order-1", 7, "需求", List.of()),
                new ProcessAiParseReservation("conversation-1", 2, "1.0.0", 1), memory,
                new com.paper.mes.ai.process.security.ProcessTextRedactionResult("UNKNOWN", List.of(), false),
                List.of(), null, 3, System.nanoTime());
    }

    private ProcessAiModelExecution execution() {
        ProcessAiExtractionResult extraction = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(), List.of(), List.of(), false, List.of());
        return new ProcessAiModelExecution(
                new ProcessAiPromptBundle(new ProcessAiModelPrompt("system", "context"), List.of()),
                new com.paper.mes.ai.process.model.ProcessAiModelResult(
                        "{}", "model", "provider", "PRIMARY", 1, 1), extraction);
    }

    private ProcessAiModelExecution blockedExecution() {
        ProcessAiExtractionResult extraction = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(), List.of("未映射"), List.of(), true, List.of());
        return new ProcessAiModelExecution(
                new ProcessAiPromptBundle(new ProcessAiModelPrompt("system", "context"), List.of()),
                new com.paper.mes.ai.process.model.ProcessAiModelResult(
                        "{}", "model", "provider", "PRIMARY", 1, 1), extraction);
    }
}
