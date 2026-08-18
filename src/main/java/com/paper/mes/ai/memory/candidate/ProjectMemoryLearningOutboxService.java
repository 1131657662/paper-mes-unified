package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectMemoryLearningOutboxService {

    private final ProjectMemoryLearningOutboxRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueueConfirmed(ProjectMemoryCandidateConfirmedEvent event) {
        enqueue("CONFIRMED_PARSE:" + event.parseId(), "CONFIRMED_PARSE", event);
    }

    @Transactional
    public void enqueueSubmitted(ProjectMemorySubmissionLearningSnapshot snapshot) {
        enqueue("SUBMITTED_ORDER:" + snapshot.orderUuid(), "SUBMITTED_ORDER", snapshot);
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
}
