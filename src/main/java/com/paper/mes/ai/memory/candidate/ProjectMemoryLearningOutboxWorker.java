package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ProjectMemoryLearningOutboxWorker {

    private static final int BATCH_SIZE = 20;
    private static final int MAX_ERROR_CHARS = 500;

    private final ProjectMemoryLearningOutboxRepository repository;
    private final ProjectMemoryCandidateCaptureService captureService;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;
    private final ProcessAiMemoryReferenceHasher referenceHasher;

    /** Compatibility constructor for focused worker tests. */
    ProjectMemoryLearningOutboxWorker(ProjectMemoryLearningOutboxRepository repository,
                                      ProjectMemoryCandidateCaptureService captureService,
                                      ObjectMapper objectMapper, AiProperties properties) {
        this(repository, captureService, objectMapper, properties, null);
    }

    @Autowired
    public ProjectMemoryLearningOutboxWorker(ProjectMemoryLearningOutboxRepository repository,
                                             ProjectMemoryCandidateCaptureService captureService,
                                             ObjectMapper objectMapper, AiProperties properties,
                                             ProcessAiMemoryReferenceHasher referenceHasher) {
        this.repository = repository;
        this.captureService = captureService;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.referenceHasher = referenceHasher;
    }

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
            PreparedPayload prepared = prepare(row);
            if (prepared.rewrittenJson() != null) {
                requireUpdated(repository.replacePayload(row.uuid(), prepared.rewrittenJson()));
            }
            capture(prepared);
            requireUpdated(repository.complete(row.uuid()));
        } catch (RuntimeException exception) {
            int attempt = row.attemptCount() + 1;
            LocalDateTime next = LocalDateTime.now().plusSeconds(backoffSeconds(attempt));
            repository.fail(row.uuid(), attempt, error(exception), next);
            log.error("Project-memory learning event deferred: eventId={}, attempt={}",
                    row.uuid(), attempt, exception);
        }
    }

    private PreparedPayload prepare(ProjectMemoryLearningOutboxRow row) {
        try {
            JsonNode payload = objectMapper.readTree(row.payloadJson());
            if ("CONFIRMED_PARSE".equals(row.eventType())) {
                ProjectMemoryCandidateLearningSnapshot snapshot = confirmedPayload(payload);
                String rewritten = payload.has("orderUuid") || payload.has("parseId")
                        ? objectMapper.writeValueAsString(snapshot) : null;
                return new PreparedPayload(snapshot, rewritten);
            }
            if ("SUBMITTED_ORDER".equals(row.eventType())) {
                ProjectMemorySubmissionLearningOutboxSnapshot snapshot = submittedPayload(payload);
                String rewritten = payload.has("orderUuid") || payload.has("customerRequirement")
                        ? objectMapper.writeValueAsString(snapshot) : null;
                return new PreparedPayload(snapshot, rewritten);
            }
            throw new IllegalStateException("unknown project-memory learning event type");
        } catch (Exception exception) {
            throw new IllegalStateException("project-memory learning event payload is invalid", exception);
        }
    }

    private void capture(PreparedPayload prepared) {
        try {
            if (prepared.snapshot() instanceof ProjectMemoryCandidateLearningSnapshot snapshot) {
                captureService.capture(snapshot);
                return;
            }
            if (prepared.snapshot() instanceof ProjectMemorySubmissionLearningOutboxSnapshot snapshot) {
                captureService.captureSubmittedOutbox(snapshot);
                return;
            }
            throw new IllegalStateException("unknown project-memory learning event type");
        } catch (Exception exception) {
            throw new IllegalStateException("project-memory learning event payload is invalid",
                    exception);
        }
    }

    private ProjectMemoryCandidateLearningSnapshot confirmedPayload(JsonNode payload)
            throws Exception {
        if (!payload.has("orderUuid") && !payload.has("parseId")) {
            return objectMapper.treeToValue(payload, ProjectMemoryCandidateLearningSnapshot.class);
        }
        ProjectMemoryCandidateConfirmedEvent legacy = objectMapper.treeToValue(
                payload, ProjectMemoryCandidateConfirmedEvent.class);
        return ProjectMemoryCandidateLearningSnapshot.from(legacy,
                hashReference(legacy.orderUuid()), hashReference(legacy.parseId()));
    }

    private ProjectMemorySubmissionLearningOutboxSnapshot submittedPayload(JsonNode payload)
            throws Exception {
        if (!payload.has("orderUuid") && !payload.has("customerRequirement")) {
            return objectMapper.treeToValue(payload, ProjectMemorySubmissionLearningOutboxSnapshot.class);
        }
        LegacySubmittedPayload legacy = objectMapper.treeToValue(payload, LegacySubmittedPayload.class);
        return new ProjectMemorySubmissionLearningOutboxSnapshot(
                hashReference(legacy.orderUuid()), legacy.projectMemoryVersion(),
                ProcessAiAuditHasher.sha256(legacy.customerRequirement()), null,
                safeFinalConfiguration(legacy.finalConfiguration()), legacy.createdBy());
    }

    private String hashReference(String value) {
        if (referenceHasher == null) {
            throw new IllegalStateException("memory reference HMAC key is not configured");
        }
        return referenceHasher.hash(value);
    }

    private JsonNode safeFinalConfiguration(JsonNode configuration) {
        ObjectNode safe = objectMapper.createObjectNode();
        ArrayNode plans = safe.putArray("processPlans");
        JsonNode source = configuration == null ? null : configuration.path("processPlans");
        if (source == null || !source.isArray()) return safe;
        source.forEach(plan -> {
            if (!plan.isObject()) return;
            ObjectNode item = plans.addObject();
            copyInteger(plan, item, "processMode");
            copyInteger(plan, item, "mainStepType");
        });
        return safe;
    }

    private void copyInteger(JsonNode source, ObjectNode target, String field) {
        JsonNode value = source.get(field);
        if (value != null && value.isIntegralNumber() && value.canConvertToInt()) {
            target.put(field, value.intValue());
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

    private record LegacySubmittedPayload(
            String orderUuid,
            String projectMemoryVersion,
            String customerRequirement,
            JsonNode rollContext,
            JsonNode finalConfiguration,
            String createdBy) {
    }

    private record PreparedPayload(Object snapshot, String rewrittenJson) {
    }
}
