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

class ProcessParamIntegrityBootstrapTest {

    @Test
    void run_whenAreaRatioColumnIsNarrow_widensColumn() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(any(String.class), eq("biz_process_param"), eq("area_ratio")))
                .thenReturn(List.of(Map.of("numeric_precision", 5, "numeric_scale", 2)));

        new ProcessParamIntegrityBootstrap(jdbcTemplate).run(null);

        verify(jdbcTemplate).execute(sqlContains("MODIFY `area_ratio` DECIMAL(10,3)"));
    }

    @Test
    void run_whenAreaRatioColumnIsWide_keepsColumn() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(any(String.class), eq("biz_process_param"), eq("area_ratio")))
                .thenReturn(List.of(Map.of("numeric_precision", 10, "numeric_scale", 3)));

        new ProcessParamIntegrityBootstrap(jdbcTemplate).run(null);

        verify(jdbcTemplate, never()).execute(any(String.class));
    }

    private String sqlContains(String text) {
        return org.mockito.ArgumentMatchers.argThat(sql -> sql != null && sql.contains(text));
    }
}
