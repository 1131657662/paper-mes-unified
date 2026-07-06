package com.paper.mes.system.config.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProcessDraftIntegrityBootstrapTest {

    @Test
    void run_createsProcessConfigDraftTableWhenMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProcessDraftIntegrityBootstrap bootstrap = new ProcessDraftIntegrityBootstrap(jdbcTemplate);

        bootstrap.run(null);

        verify(jdbcTemplate).execute(sqlContains("CREATE TABLE IF NOT EXISTS `biz_process_config_draft`"));
        verify(jdbcTemplate).execute(sqlContains("UNIQUE KEY `uk_config_draft_roll`"));
    }

    private String sqlContains(String text) {
        return org.mockito.ArgumentMatchers.argThat(sql -> sql != null && sql.contains(text));
    }
}
