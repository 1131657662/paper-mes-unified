package com.paper.mes.ai.process.audit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProcessAiCallAuditRepositoryTest {

    @Test
    void insertUsesParameterizedSqlAndKeepsEveryAttemptAppendOnly() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProcessAiCallAuditRepository repository = new ProcessAiCallAuditRepository(jdbcTemplate);

        repository.insert(entry(), "[]");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sql.capture(), any(Object[].class));
        assertThat(sql.getValue())
                .contains("INSERT INTO sys_ai_call_audit", "VALUES")
                .doesNotContain("ON DUPLICATE KEY UPDATE", "request-1");
    }

    private ProcessAiCallAuditEntry entry() {
        return ProcessAiCallAuditEntry.builder()
                .orderUuid("order-1")
                .action("START")
                .idempotencyKey("request-1")
                .projectMemoryItemIds(List.of())
                .requestHash("a".repeat(64))
                .provider("DEEPSEEK")
                .model("deepseek-v4-pro")
                .route("PRO")
                .outcome("FAILED")
                .build();
    }
}
