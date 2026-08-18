package com.paper.mes.ai.process.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessAiCallAuditService {

    private final ProcessAiCallAuditRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ProcessAiCallAuditEntry entry) {
        repository.insert(entry, memoryItemsJson(entry));
    }

    private String memoryItemsJson(ProcessAiCallAuditEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry.projectMemoryItemIds());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("AI audit metadata could not be stored", ex);
        }
    }
}
