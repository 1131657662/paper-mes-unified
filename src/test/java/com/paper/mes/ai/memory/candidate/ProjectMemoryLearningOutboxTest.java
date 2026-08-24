package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiEvidence;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiSawIntent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class ProjectMemoryLearningOutboxTest {

    @Test
    void workerCompletesASubmittedOrderEventAfterCaptureSucceeds() {
        ProjectMemoryLearningOutboxRepository repository = mock(
                ProjectMemoryLearningOutboxRepository.class);
        ProjectMemoryCandidateCaptureService capture = mock(
                ProjectMemoryCandidateCaptureService.class);
        ProjectMemoryLearningOutboxRow row = row("SUBMITTED_ORDER", "{}", 0);
        when(repository.findDue(20)).thenReturn(List.of(row));
        when(repository.claim(row.uuid())).thenReturn(1);
        when(repository.complete(row.uuid())).thenReturn(1);
        when(repository.replacePayload(org.mockito.ArgumentMatchers.eq(row.uuid()),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(1);

        new ProjectMemoryLearningOutboxWorker(
                repository, capture, new ObjectMapper(), new AiProperties()).processDue();

        verify(capture).captureSubmittedOutbox(any());
        verify(repository).complete(row.uuid());
        verify(repository, never()).fail(anyString(), anyInt(), anyString(), any());
    }

    @Test
    void workerPersistsRetryStateWhenCaptureFails() {
        ProjectMemoryLearningOutboxRepository repository = mock(
                ProjectMemoryLearningOutboxRepository.class);
        ProjectMemoryCandidateCaptureService capture = mock(
                ProjectMemoryCandidateCaptureService.class);
        ProjectMemoryLearningOutboxRow row = row("SUBMITTED_ORDER", "{}", 2);
        when(repository.findDue(20)).thenReturn(List.of(row));
        when(repository.claim(row.uuid())).thenReturn(1);
        org.mockito.Mockito.doThrow(new IllegalStateException("memory unavailable"))
                .when(capture).captureSubmittedOutbox(any());

        new ProjectMemoryLearningOutboxWorker(
                repository, capture, new ObjectMapper(), new AiProperties()).processDue();

        verify(repository).fail(anyString(), org.mockito.ArgumentMatchers.eq(3),
                anyString(), any(LocalDateTime.class));
        verify(repository, never()).complete(anyString());
    }

    @Test
    void workerRetriesCaptureWhenCompletionFailsAfterCapture() {
        ProjectMemoryLearningOutboxRepository repository = mock(
                ProjectMemoryLearningOutboxRepository.class);
        ProjectMemoryCandidateCaptureService capture = mock(
                ProjectMemoryCandidateCaptureService.class);
        ProjectMemoryLearningOutboxRow first = row("SUBMITTED_ORDER", "{}", 0);
        ProjectMemoryLearningOutboxRow retry = new ProjectMemoryLearningOutboxRow(
                "outbox-2", "event-2", "SUBMITTED_ORDER", "{}", 1, LocalDateTime.now());
        when(repository.findDue(20)).thenReturn(List.of(first), List.of(retry));
        when(repository.claim(first.uuid())).thenReturn(1);
        when(repository.claim(retry.uuid())).thenReturn(1);
        when(repository.complete(first.uuid())).thenReturn(0);
        when(repository.complete(retry.uuid())).thenReturn(1);

        ProjectMemoryLearningOutboxWorker worker = new ProjectMemoryLearningOutboxWorker(
                repository, capture, new ObjectMapper(), new AiProperties());

        worker.processDue();
        worker.processDue();

        verify(capture, times(2)).captureSubmittedOutbox(any());
        verify(repository).fail(eq(first.uuid()), eq(1), anyString(), any(LocalDateTime.class));
        verify(repository).complete(first.uuid());
        verify(repository).complete(retry.uuid());
    }

    @Test
    void enqueueUsesAnIdempotentEventKeyAndJsonPayload() {
        ProjectMemoryLearningOutboxRepository repository = mock(
                ProjectMemoryLearningOutboxRepository.class);
        ProjectMemoryLearningOutboxService service = service(repository, new ObjectMapper());
        ProjectMemorySubmissionLearningSnapshot snapshot =
                new ProjectMemorySubmissionLearningSnapshot(
                        "order-1", "1.0.0", "客户原话",
                        new ObjectMapper().createObjectNode(), new ObjectMapper().createObjectNode(),
                        "admin");

        service.enqueueSubmitted(snapshot);

        var payload = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository).enqueue(anyString(),
                org.mockito.ArgumentMatchers.argThat(value -> value.startsWith("SUBMITTED_ORDER:")
                        && !value.contains("order-1")),
                org.mockito.ArgumentMatchers.eq("SUBMITTED_ORDER"), payload.capture());
        assertThat(payload.getValue()).doesNotContain("客户原话", "order-1");
    }

    @Test
    void submittedPayloadKeepsOnlyTheFieldsUsedByManualLearning() throws Exception {
        ProjectMemoryLearningOutboxRepository repository = mock(
                ProjectMemoryLearningOutboxRepository.class);
        ObjectMapper mapper = new ObjectMapper();
        ProjectMemoryLearningOutboxService service = service(repository, mapper);
        var finalConfiguration = mapper.readTree("""
                {"processPlans":[{"processMode":1,"mainStepType":2,
                 "source":{"paperName":"客户机密"},"config":{"orderUuid":"order-1"}}],
                 "rawCustomerText":"客户原话"}
                """);
        var rollContext = mapper.readTree("""
                {"sourceRolls":[{"paperName":"客户机密","originalUuid":"roll-1"}]}
                """);
        service.enqueueSubmitted(new ProjectMemorySubmissionLearningSnapshot(
                "order-1", "1.0.0", "客户原话", rollContext, finalConfiguration, "admin"));

        var payload = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository).enqueue(anyString(), anyString(),
                org.mockito.ArgumentMatchers.eq("SUBMITTED_ORDER"), payload.capture());
        assertThat(payload.getValue()).doesNotContain("客户机密", "客户原话", "order-1", "roll-1")
                .contains("processMode", "mainStepType")
                .doesNotContain("source", "config", "rawCustomerText", "sourceRolls");
    }

    @Test
    void submittedEventsWithDifferentFinalConfigurationsUseDistinctIdempotencyKeys() {
        ProjectMemoryLearningOutboxRepository repository = mock(
                ProjectMemoryLearningOutboxRepository.class);
        ObjectMapper mapper = new ObjectMapper();
        ProjectMemoryLearningOutboxService service = service(repository, mapper);
        var first = mapper.createObjectNode();
        first.putArray("processPlans").addObject().put("processMode", 1).put("mainStepType", 2);
        var second = mapper.createObjectNode();
        second.putArray("processPlans").addObject().put("processMode", 3).put("mainStepType", 2);

        service.enqueueSubmitted(new ProjectMemorySubmissionLearningSnapshot(
                "order-1", "1.0.0", "客户原话", null, first, "admin"));
        service.enqueueSubmitted(new ProjectMemorySubmissionLearningSnapshot(
                "order-1", "1.0.0", "客户原话", null, second, "admin"));

        var keys = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository, org.mockito.Mockito.times(2)).enqueue(anyString(), keys.capture(),
                org.mockito.ArgumentMatchers.eq("SUBMITTED_ORDER"), anyString());
        assertThat(keys.getAllValues()).hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    void confirmedPayloadContainsOnlyHashedReferencesAndStructuredExtraction() {
        ProjectMemoryLearningOutboxRepository repository = mock(
                ProjectMemoryLearningOutboxRepository.class);
        ProjectMemoryLearningOutboxService service = service(repository, new ObjectMapper());
        ProcessAiAssignment assignment = new ProcessAiAssignment(List.of("R1"), "R1", List.of(),
                "SAW", null, new ProcessAiSawIntent("CUTS", 2, null, "mm"), null,
                List.of(new ProcessAiEvidence("sawIntent", "客户原话：切两刀")));
        ProjectMemoryCandidateConfirmedEvent event = new ProjectMemoryCandidateConfirmedEvent(
                "order-secret", "parse-secret", "1.0.0", "客户原话：切两刀", "admin",
                new ProcessAiExtractionResult("parse-secret", "1.0", List.of(assignment),
                        List.of(), List.of(), false, List.of()), List.of("/assignments/R1/sawIntent/type"),
                null, null);

        service.enqueueConfirmed(event);

        var payload = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository).enqueue(anyString(), anyString(),
                org.mockito.ArgumentMatchers.eq("CONFIRMED_PARSE"), payload.capture());
        assertThat(payload.getValue()).doesNotContain("order-secret", "parse-secret", "客户原话：切两刀")
                .contains("redacted", "sawIntent");
    }

    @Test
    void workerRecoversStaleProcessingRowsBeforePolling() {
        ProjectMemoryLearningOutboxRepository repository = mock(
                ProjectMemoryLearningOutboxRepository.class);
        ProjectMemoryCandidateCaptureService capture = mock(
                ProjectMemoryCandidateCaptureService.class);

        new ProjectMemoryLearningOutboxWorker(
                repository, capture, new ObjectMapper(), new AiProperties()).processDue();

        verify(repository).requeueStaleProcessing(any(LocalDateTime.class));
        verify(repository).findDue(20);
    }

    @Test
    void workerRehydratesLegacySubmittedPayloadWithHashedReferences() throws Exception {
        ProjectMemoryLearningOutboxRepository repository = mock(
                ProjectMemoryLearningOutboxRepository.class);
        ProjectMemoryCandidateCaptureService capture = mock(
                ProjectMemoryCandidateCaptureService.class);
        ProjectMemoryLearningOutboxRow row = row("SUBMITTED_ORDER", """
                {"orderUuid":"order-legacy","projectMemoryVersion":"1.0.0",
                 "customerRequirement":"客户原话","rollContext":{"raw":"secret"},
                 "finalConfiguration":{"processPlans":[{"processMode":1,"mainStepType":2,
                   "source":{"orderUuid":"order-legacy"}}]},"createdBy":"admin"}
                """, 0);
        when(repository.findDue(20)).thenReturn(List.of(row));
        when(repository.claim(row.uuid())).thenReturn(1);
        when(repository.complete(row.uuid())).thenReturn(1);
        when(repository.replacePayload(org.mockito.ArgumentMatchers.eq(row.uuid()),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(1);
        AiProperties properties = new AiProperties();
        properties.setMemoryReferenceHmacKey("01234567890123456789012345678901");

        new ProjectMemoryLearningOutboxWorker(repository, capture, new ObjectMapper(), properties,
                new ProcessAiMemoryReferenceHasher(properties)).processDue();

        var snapshot = org.mockito.ArgumentCaptor.forClass(
                ProjectMemorySubmissionLearningOutboxSnapshot.class);
        verify(capture).captureSubmittedOutbox(snapshot.capture());
        var rewritten = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository).replacePayload(org.mockito.ArgumentMatchers.eq(row.uuid()), rewritten.capture());
        assertThat(rewritten.getValue()).doesNotContain("order-legacy", "客户原话", "secret")
                .contains("orderRefHash", "customerRequirementHash", "processMode");
        assertThat(snapshot.getValue().orderRefHash()).doesNotContain("order-legacy")
                .hasSize(64);
        assertThat(snapshot.getValue().customerRequirementHash()).hasSize(64);
        assertThat(snapshot.getValue().finalConfiguration().toString())
                .doesNotContain("order-legacy", "secret")
                .contains("processMode", "mainStepType");
    }

    @Test
    void workerRehydratesLegacyConfirmedPayloadWithHashedReferences() {
        ProjectMemoryLearningOutboxRepository repository = mock(
                ProjectMemoryLearningOutboxRepository.class);
        ProjectMemoryCandidateCaptureService capture = mock(
                ProjectMemoryCandidateCaptureService.class);
        ProjectMemoryLearningOutboxRow row = row("CONFIRMED_PARSE", """
                {"orderUuid":"order-legacy","parseId":"parse-legacy",
                 "projectMemoryVersion":"1.0.0","customerRequirementHash":"客户原话",
                 "confirmedBy":"admin","extraction":null,"acceptedFieldPaths":[],
                 "compilation":null,"baseline":null,"corrections":[],
                 "effectiveDefaults":[],"previewHash":null}
                """, 0);
        when(repository.findDue(20)).thenReturn(List.of(row));
        when(repository.claim(row.uuid())).thenReturn(1);
        when(repository.complete(row.uuid())).thenReturn(1);
        when(repository.replacePayload(org.mockito.ArgumentMatchers.eq(row.uuid()),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(1);
        AiProperties properties = new AiProperties();
        properties.setMemoryReferenceHmacKey("01234567890123456789012345678901");

        new ProjectMemoryLearningOutboxWorker(repository, capture, new ObjectMapper(), properties,
                new ProcessAiMemoryReferenceHasher(properties)).processDue();

        var snapshot = org.mockito.ArgumentCaptor.forClass(ProjectMemoryCandidateLearningSnapshot.class);
        verify(capture).capture(snapshot.capture());
        var rewritten = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository).replacePayload(org.mockito.ArgumentMatchers.eq(row.uuid()), rewritten.capture());
        assertThat(rewritten.getValue()).doesNotContain("order-legacy", "parse-legacy", "客户原话")
                .contains("orderRefHash", "parseRefHash");
        assertThat(snapshot.getValue().orderRefHash()).doesNotContain("order-legacy")
                .hasSize(64);
        assertThat(snapshot.getValue().parseRefHash()).doesNotContain("parse-legacy")
                .hasSize(64);
    }

    private ProjectMemoryLearningOutboxRow row(String type, String payload, int attempts) {
        return new ProjectMemoryLearningOutboxRow(
                "outbox-1", "event-1", type, payload, attempts, LocalDateTime.now());
    }

    private ProjectMemoryLearningOutboxService service(
            ProjectMemoryLearningOutboxRepository repository, ObjectMapper mapper) {
        AiProperties properties = new AiProperties();
        properties.setMemoryReferenceHmacKey("01234567890123456789012345678901");
        return new ProjectMemoryLearningOutboxService(repository, mapper,
                new ProcessAiMemoryReferenceHasher(properties));
    }
}
