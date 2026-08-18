package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.audit.ProcessAiCallAuditEntry;
import com.paper.mes.ai.process.audit.ProcessAiCallAuditService;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProcessAiConfirmationAuditRecorderTest {

    @Test
    void recordLinksConfirmationToTheParseMemoryAndAcceptedFields() {
        ProcessAiCallAuditService auditService = mock(ProcessAiCallAuditService.class);
        ProcessAiConfirmationCodec codec = new ProcessAiConfirmationCodec(
                ProcessAiConfirmationTestFixtures.mapper(),
                Validation.buildDefaultValidatorFactory().getValidator(),
                ProcessAiConfirmationTestFixtures.structuredCipher(),
                ProcessAiConfirmationTestFixtures.intentCipher());
        ProcessAiConfirmationAuditRecorder recorder =
                new ProcessAiConfirmationAuditRecorder(auditService, codec);

        recorder.record(preparation(), response());

        ArgumentCaptor<ProcessAiCallAuditEntry> captor =
                ArgumentCaptor.forClass(ProcessAiCallAuditEntry.class);
        verify(auditService).record(captor.capture());
        ProcessAiCallAuditEntry entry = captor.getValue();
        assertThat(entry.action()).isEqualTo("CONFIRM");
        assertThat(entry.idempotencyKey()).isEqualTo("apply-1");
        assertThat(entry.projectMemoryItemIds()).containsExactly("rule-saw");
        assertThat(entry.resultHash()).isEqualTo("c".repeat(64));
        assertThat(entry.requestHash()).hasSize(64);
    }

    private ProcessAiConfirmationPreparation preparation() {
        ProcessAiParseRecord record = ProcessAiConfirmationTestFixtures.record(
                ProcessAiConfirmationTestFixtures.mapper(),
                "READY", ProcessAiParseConfirmation.empty());
        ProcessAiConfirmationLoad load = new ProcessAiConfirmationLoad(
                record, ProcessAiConfirmationTestFixtures.extraction(),
                List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH), "apply-1", null);
        return new ProcessAiConfirmationPreparation("user-1", load, null, null, null);
    }

    private ProcessAiConfirmResponse response() {
        return new ProcessAiConfirmResponse(
                "conversation-1", "parse-1", 2, 7, 8, "CONFIRMED",
                List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH), Map.of(),
                List.of(), List.of(), "customer requirement", "c".repeat(64));
    }
}
