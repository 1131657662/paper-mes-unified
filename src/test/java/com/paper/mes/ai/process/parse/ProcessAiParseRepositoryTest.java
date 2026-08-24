package com.paper.mes.ai.process.parse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiParseRepositoryTest {

    @Test
    void insertBindsEverySqlPlaceholderIncludingWorkflowVersion() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        ProcessAiParseRepository repository = new ProcessAiParseRepository(jdbcTemplate);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parameters = ArgumentCaptor.forClass(Object[].class);

        int updated = repository.insert(ProcessAiConfirmationTestFixtures.record(
                ProcessAiConfirmationTestFixtures.mapper(), "READY",
                ProcessAiParseConfirmation.empty()));

        verify(jdbcTemplate).update(sql.capture(), parameters.capture());
        assertThat(updated).isEqualTo(1);
        assertThat(parameters.getValue()).hasSize(questionMarkCount(sql.getValue()));
        assertThat(parameters.getValue()[19]).isEqualTo(1);
    }

    private int questionMarkCount(String sql) {
        return (int) sql.chars().filter(character -> character == '?').count();
    }
}
