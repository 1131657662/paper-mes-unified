package com.paper.mes.system.config.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogIntegrityBootstrapTest {

    @Test
    void run_whenRemarkColumnIsVarchar_widensToText() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(any(String.class), eq("sys_operation_log"), eq("remark")))
                .thenReturn(List.of(Map.of("data_type", "varchar")));

        new OperationLogIntegrityBootstrap(jdbcTemplate).run(null);

        verify(jdbcTemplate).execute(sqlContains("MODIFY `remark` TEXT"));
    }

    @Test
    void run_whenRemarkColumnIsAlreadyText_keepsColumn() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(any(String.class), eq("sys_operation_log"), eq("remark")))
                .thenReturn(List.of(Map.of("data_type", "text")));

        new OperationLogIntegrityBootstrap(jdbcTemplate).run(null);

        verify(jdbcTemplate, never()).execute(any(String.class));
    }

    private String sqlContains(String text) {
        return org.mockito.ArgumentMatchers.argThat(sql -> sql != null && sql.contains(text));
    }
}
