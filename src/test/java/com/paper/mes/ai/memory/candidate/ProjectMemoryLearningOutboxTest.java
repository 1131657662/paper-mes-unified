package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        new ProjectMemoryLearningOutboxWorker(
                repository, capture, new ObjectMapper(), new AiProperties()).processDue();

        verify(capture).captureSubmission(any());
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
                .when(capture).captureSubmission(any());

        new ProjectMemoryLearningOutboxWorker(
                repository, capture, new ObjectMapper(), new AiProperties()).processDue();

        verify(repository).fail(anyString(), org.mockito.ArgumentMatchers.eq(3),
                anyString(), any(LocalDateTime.class));
        verify(repository, never()).complete(anyString());
    }

    @Test
    void enqueueUsesAnIdempotentEventKeyAndJsonPayload() {
        ProjectMemoryLearningOutboxRepository repository = mock(
                ProjectMemoryLearningOutboxRepository.class);
        ProjectMemoryLearningOutboxService service = new ProjectMemoryLearningOutboxService(
                repository, new ObjectMapper());
        ProjectMemorySubmissionLearningSnapshot snapshot =
                new ProjectMemorySubmissionLearningSnapshot(
                        "order-1", "1.0.0", "客户原话",
                        new ObjectMapper().createObjectNode(), new ObjectMapper().createObjectNode(),
                        "admin");

        service.enqueueSubmitted(snapshot);

        verify(repository).enqueue(anyString(),
                org.mockito.ArgumentMatchers.eq("SUBMITTED_ORDER:order-1"),
                org.mockito.ArgumentMatchers.eq("SUBMITTED_ORDER"), anyString());
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

    private ProjectMemoryLearningOutboxRow row(String type, String payload, int attempts) {
        return new ProjectMemoryLearningOutboxRow(
                "outbox-1", "event-1", type, payload, attempts, LocalDateTime.now());
    }
}
