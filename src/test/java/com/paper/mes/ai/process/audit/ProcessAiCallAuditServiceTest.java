package com.paper.mes.ai.process.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProcessAiCallAuditServiceTest {

    @Test
    void recordSerializesOnlyMemoryItemIdentifiersForPersistence() {
        ProcessAiCallAuditRepository repository = mock(ProcessAiCallAuditRepository.class);
        ProcessAiCallAuditService service = new ProcessAiCallAuditService(
                repository, new ObjectMapper());
        ProcessAiCallAuditEntry entry = ProcessAiCallAuditEntry.builder()
                .orderUuid("order-1")
                .action("START")
                .idempotencyKey("request-1")
                .projectMemoryItemIds(List.of("rule-saw", "term-cut"))
                .requestHash("a".repeat(64))
                .provider("DEEPSEEK")
                .model("deepseek-v4-pro")
                .route("PRO")
                .outcome("READY")
                .build();

        service.record(entry);

        verify(repository).insert(entry, "[\"rule-saw\",\"term-cut\"]");
    }
}
