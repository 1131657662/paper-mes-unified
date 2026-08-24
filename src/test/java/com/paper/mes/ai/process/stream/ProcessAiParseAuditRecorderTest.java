package com.paper.mes.ai.process.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.audit.ProcessAiCallAuditEntry;
import com.paper.mes.ai.process.audit.ProcessAiCallAuditService;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiEvidence;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiSawIntent;
import com.paper.mes.ai.process.model.ProcessAiModelPrompt;
import com.paper.mes.ai.process.model.ProcessAiModelResult;
import com.paper.mes.ai.process.parse.ProcessAiParseConfirmation;
import com.paper.mes.ai.process.parse.ProcessAiParseRecord;
import com.paper.mes.ai.process.prompt.ProcessAiPromptBundle;
import com.paper.mes.ai.process.security.ProcessTextRedactionResult;
import com.paper.mes.ai.process.session.dto.ProcessAiParseReservation;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseStreamRequest;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ProcessAiParseAuditRecorderTest {

    private final ProcessAiCallAuditService auditService = mock(ProcessAiCallAuditService.class);
    private ProcessAiParseAuditRecorder recorder;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        properties.setProvider("DEEPSEEK");
        properties.setDeepseekApiKey("test-key");
        recorder = new ProcessAiParseAuditRecorder(auditService, properties);
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1").build());
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void successRecordsTokensAndHashesWithoutPersistingTheSanitizedText() {
        recorder.success(new ProcessAiParseAuditSuccess(
                prepared(), execution(), parseRecord(), "READY"));

        ProcessAiCallAuditEntry entry = capturedEntry();
        assertThat(entry.action()).isEqualTo("START");
        assertThat(entry.requestHash()).hasSize(64).doesNotContain("cut twice");
        assertThat(entry.projectMemoryItemIds()).containsExactly("rule-saw");
        assertThat(entry.inputTokens()).isEqualTo(120);
        assertThat(entry.outputTokens()).isEqualTo(30);
        assertThat(entry.provider()).isEqualTo("DEEPSEEK");
        assertThat(entry.route()).isEqualTo("PRO");
        assertThat(entry.createdBy()).isEqualTo("user-1");
    }

    @Test
    void failureRecordsTheFailureCodeForTheSameLogicalRequest() {
        recorder.failure(prepared(), "AI_PROVIDER_RATE_LIMITED");

        ProcessAiCallAuditEntry entry = capturedEntry();
        assertThat(entry.outcome()).isEqualTo("FAILED");
        assertThat(entry.failureCode()).isEqualTo("AI_PROVIDER_RATE_LIMITED");
        assertThat(entry.model()).isEqualTo("deepseek-v4-pro");
    }

    @Test
    void successAfterCommitDoesNotJoinTheParseTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            recorder.successAfterCommit(new ProcessAiParseAuditSuccess(
                    prepared(), execution(), parseRecord(), "READY"));

            verify(auditService, never()).record(org.mockito.ArgumentMatchers.any());
            TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();
            verify(auditService).record(org.mockito.ArgumentMatchers.any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private ProcessAiCallAuditEntry capturedEntry() {
        ArgumentCaptor<ProcessAiCallAuditEntry> captor =
                ArgumentCaptor.forClass(ProcessAiCallAuditEntry.class);
        verify(auditService).record(captor.capture());
        return captor.getValue();
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

    private ProcessAiModelExecution execution() {
        ProcessAiPromptBundle prompt = new ProcessAiPromptBundle(
                new ProcessAiModelPrompt("system", "context"), List.of("rule-saw"));
        ProcessAiModelResult model = new ProcessAiModelResult(
                "{}", "deepseek-v4-pro-202608", "DEEPSEEK", "PRO", 120, 30);
        return new ProcessAiModelExecution(prompt, model, extraction());
    }

    private ProcessAiExtractionResult extraction() {
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW", null,
                new ProcessAiSawIntent("CUTS", 2, null, null), null,
                List.of(new ProcessAiEvidence("knifeCount", "cut twice")));
        return new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment),
                List.of(), List.of(), false, List.of());
    }

    private ProcessAiParseRecord parseRecord() {
        return new ProcessAiParseRecord(
                "row-1", "order-1", "conversation-1", "parse-1", 2, 1,
                "request-1", 7, "READY", "DEEPSEEK", "deepseek-v4-pro", "PRO",
                "1.0", "1.0.0", "sha256:" + "a".repeat(64), "[\"rule-saw\"]",
                "{}", "b".repeat(64), ProcessAiParseConfirmation.empty(),
                LocalDateTime.now());
    }
}
