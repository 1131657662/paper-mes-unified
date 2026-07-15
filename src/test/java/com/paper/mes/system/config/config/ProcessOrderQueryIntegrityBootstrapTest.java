package com.paper.mes.system.config.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessOrderQueryIntegrityBootstrapTest {

    @Test
    void run_whenListIndexIsMissing_addsIndex() {
        JdbcTemplate jdbcTemplate = jdbcTemplateWithIndexCount(0);

        new ProcessOrderQueryIntegrityBootstrap(jdbcTemplate).run(null);

        verify(jdbcTemplate).execute(sqlContains("idx_customer_deleted_ctime"));
    }

    @Test
    void run_whenListIndexExists_keepsSchema() {
        JdbcTemplate jdbcTemplate = jdbcTemplateWithIndexCount(1);

        new ProcessOrderQueryIntegrityBootstrap(jdbcTemplate).run(null);

        verify(jdbcTemplate, never()).execute(any(String.class));
    }

    private JdbcTemplate jdbcTemplateWithIndexCount(int count) {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class),
                eq(ProcessOrderQueryIntegrityBootstrap.TABLE), eq(ProcessOrderQueryIntegrityBootstrap.INDEX)))
                .thenReturn(count);
        return jdbcTemplate;
    }

    private String sqlContains(String text) {
        return org.mockito.ArgumentMatchers.argThat(sql -> sql != null && sql.contains(text));
    }
}
