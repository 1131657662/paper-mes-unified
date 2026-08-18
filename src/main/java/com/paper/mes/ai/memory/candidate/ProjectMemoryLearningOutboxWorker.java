package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemoryLearningOutboxWorker {

    private static final int BATCH_SIZE = 20;
    private static final int MAX_ERROR_CHARS = 500;

    private final ProjectMemoryLearningOutboxRepository repository;
    private final ProjectMemoryCandidateCaptureService captureService;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    @Scheduled(fixedDelayString = "${app.ai.memory.learning-retry-ms:30000}")
    public void processDue() {
        repository.requeueStaleProcessing(LocalDateTime.now().minusMinutes(
                properties.getMemoryLearningProcessingTimeoutMinutes()));
        List<ProjectMemoryLearningOutboxRow> rows = repository.findDue(BATCH_SIZE);
        rows.forEach(this::process);
    }

    private void process(ProjectMemoryLearningOutboxRow row) {
        if (repository.claim(row.uuid()) != 1) return;
        try {
            capture(row);
            requireUpdated(repository.complete(row.uuid()));
        } catch (RuntimeException exception) {
            int attempt = row.attemptCount() + 1;
            LocalDateTime next = LocalDateTime.now().plusSeconds(backoffSeconds(attempt));
            repository.fail(row.uuid(), attempt, error(exception), next);
            log.error("Project-memory learning event deferred: eventKey={}, attempt={}",
                    row.eventKey(), attempt, exception);
        }
    }

    private void capture(ProjectMemoryLearningOutboxRow row) {
        try {
            if ("CONFIRMED_PARSE".equals(row.eventType())) {
                captureService.capture(objectMapper.readValue(
                        row.payloadJson(), ProjectMemoryCandidateConfirmedEvent.class));
                return;
            }
            if ("SUBMITTED_ORDER".equals(row.eventType())) {
                captureService.captureSubmission(objectMapper.readValue(
                        row.payloadJson(), ProjectMemorySubmissionLearningSnapshot.class));
                return;
            }
            throw new IllegalStateException("unknown project-memory learning event type");
        } catch (Exception exception) {
            throw new IllegalStateException("project-memory learning event payload is invalid",
                    exception);
        }
    }

    private long backoffSeconds(int attempt) {
        return Math.min(3_600L, 30L * (1L << Math.min(attempt, 7)));
    }

    private String error(RuntimeException exception) {
        String value = exception.getClass().getSimpleName() + ": " +
                (exception.getMessage() == null ? "unknown failure" : exception.getMessage());
        return value.length() <= MAX_ERROR_CHARS ? value : value.substring(0, MAX_ERROR_CHARS);
    }

    private void requireUpdated(int count) {
        if (count != 1) throw new IllegalStateException("learning outbox update failed");
    }
}
