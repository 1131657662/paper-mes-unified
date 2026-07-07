package com.paper.mes.system.config.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessDraftIntegrityBootstrapTest {

    @Test
    void run_createsProcessConfigDraftTableWhenMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(any(String.class), eq("biz_process_config_draft"), eq("version")))
                .thenReturn(List.of());
        ProcessDraftIntegrityBootstrap bootstrap = new ProcessDraftIntegrityBootstrap(jdbcTemplate);

        bootstrap.run(null);

        verify(jdbcTemplate).execute(sqlContains("CREATE TABLE IF NOT EXISTS `biz_process_config_draft`"));
        verify(jdbcTemplate).execute(sqlContains("`version` INT NOT NULL DEFAULT 1"));
        verify(jdbcTemplate).execute(sqlContains("UNIQUE KEY `uk_config_draft_roll`"));
    }

    @Test
    void run_whenVersionDefaultIsZero_fixesDefaultAndRows() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(any(String.class), eq("biz_process_config_draft"), eq("version")))
                .thenReturn(List.of(Map.of("column_default", "0")));
        ProcessDraftIntegrityBootstrap bootstrap = new ProcessDraftIntegrityBootstrap(jdbcTemplate);

        bootstrap.run(null);

        verify(jdbcTemplate).execute(sqlContains("MODIFY `version` INT NOT NULL DEFAULT 1"));
        verify(jdbcTemplate).update("UPDATE `biz_process_config_draft` SET `version` = 1 WHERE `version` = 0");
    }

    private String sqlContains(String text) {
        return org.mockito.ArgumentMatchers.argThat(sql -> sql != null && sql.contains(text));
    }
}
