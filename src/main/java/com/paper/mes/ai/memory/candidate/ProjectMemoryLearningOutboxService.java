package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProjectMemoryLearningOutboxService {

    private final ProjectMemoryLearningOutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final ProcessAiMemoryReferenceHasher referenceHasher;

    @Autowired
    public ProjectMemoryLearningOutboxService(ProjectMemoryLearningOutboxRepository repository,
                                              ObjectMapper objectMapper,
                                              ProcessAiMemoryReferenceHasher referenceHasher) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.referenceHasher = referenceHasher;
    }

    @Transactional
    public void enqueueConfirmed(ProjectMemoryCandidateConfirmedEvent event) {
        String orderRefHash = hashReference(event.orderUuid());
        String parseRefHash = hashReference(event.parseId());
        ProjectMemoryCandidateLearningSnapshot snapshot =
                ProjectMemoryCandidateLearningSnapshot.from(event, orderRefHash, parseRefHash);
        enqueue("CONFIRMED_PARSE:" + parseRefHash, "CONFIRMED_PARSE", snapshot);
    }

    @Transactional
    public void enqueueSubmitted(ProjectMemorySubmissionLearningSnapshot snapshot) {
        ProjectMemorySubmissionLearningOutboxSnapshot outboxSnapshot =
                new ProjectMemorySubmissionLearningOutboxSnapshot(hashReference(snapshot.orderUuid()),
                        snapshot.projectMemoryVersion(), snapshot.customerRequirementHash(),
                        null, safeFinalConfiguration(snapshot.finalConfiguration()), snapshot.createdBy());
        // An order may be returned to draft and submitted again with a different final
        // configuration. Keep retries of the same submission idempotent without dropping
        // a later submission for the same order.
        enqueue("SUBMITTED_ORDER:" + outboxSnapshot.orderRefHash() + ":"
                + submissionFingerprint(outboxSnapshot), "SUBMITTED_ORDER", outboxSnapshot);
    }

    private String submissionFingerprint(ProjectMemorySubmissionLearningOutboxSnapshot snapshot) {
        try {
            return ProcessAiAuditHasher.sha256(objectMapper.writeValueAsString(snapshot));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("project-memory submission fingerprint failed", exception);
        }
    }

    private void enqueue(String eventKey, String eventType, Object event) {
        try {
            repository.enqueue(UUID.randomUUID().toString(), eventKey, eventType,
                    objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("project-memory learning event serialization failed",
                    exception);
        }
    }

    private String hashReference(String value) {
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
}
