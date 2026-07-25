package com.paper.mes.processorder.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessOrderSettlementSchemaBootstrapTest {

    @Test
    void run_whenColumnsMissing_addsSettlementProvenanceColumns() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class),
                eq("biz_process_order"), any(String.class))).thenReturn(0);

        new ProcessOrderSettlementSchemaBootstrap(jdbcTemplate).run(null);

        verify(jdbcTemplate, times(6)).execute(any(String.class));
        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.<String>argThat(
                sql -> sql.contains("ADD COLUMN settle_source ")));
        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.<String>argThat(
                sql -> sql.contains("ADD COLUMN settle_customer_version ")));
        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.<String>argThat(
                sql -> sql.contains("ADD COLUMN settle_override_reason ")));
        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.<String>argThat(
                sql -> sql.contains("ADD CONSTRAINT chk_order_settle_source ")));
        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.<String>argThat(
                sql -> sql.contains("ADD CONSTRAINT chk_order_settle_override_reason ")));
    }

    @Test
    void run_whenColumnsExist_isIdempotent() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class),
                eq("biz_process_order"), any(String.class))).thenReturn(1);

        new ProcessOrderSettlementSchemaBootstrap(jdbcTemplate).run(null);

        verify(jdbcTemplate, never()).execute(any(String.class));
    }
}
