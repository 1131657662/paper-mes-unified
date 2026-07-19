package com.paper.mes.system.config.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessOrderAccountingDateBootstrapTest {

    @Test
    void run_whenSchemaIsMissing_addsGeneratedColumnAndIndex() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class), any(String.class), any(String.class)))
                .thenReturn(0);

        new ProcessOrderAccountingDateBootstrap(jdbcTemplate).run(null);

        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.<String>argThat(sql ->
                sql.contains("ADD COLUMN `accounting_date`") && sql.contains("GENERATED ALWAYS")));
        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.<String>argThat(sql ->
                sql.contains("idx_order_customer_status_accounting")));
    }

    @Test
    void run_whenSchemaExists_isIdempotent() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class), any(String.class), any(String.class)))
                .thenReturn(1);

        new ProcessOrderAccountingDateBootstrap(jdbcTemplate).run(null);

        verify(jdbcTemplate, never()).execute(any(String.class));
        verify(jdbcTemplate, atLeast(2)).queryForObject(any(String.class), eq(Integer.class),
                any(String.class), any(String.class));
    }
}
