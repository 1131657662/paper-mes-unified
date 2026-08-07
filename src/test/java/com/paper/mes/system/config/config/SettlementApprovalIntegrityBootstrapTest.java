package com.paper.mes.system.config.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettlementApprovalIntegrityBootstrapTest {

    @Test
    void runInvalidatesLegacyActiveApprovalsThatLackCompleteReceiptPlans() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class),
                any(String.class), any(String.class))).thenReturn(1);

        new SettlementApprovalIntegrityBootstrap(jdbcTemplate).run(null);

        verify(jdbcTemplate).update(sqlContaining(
                "WHERE policy_version='legacy-v1' AND approval_status IN (1,2)"));
        verify(jdbcTemplate).execute(sqlContaining("chk_discount_approval_components"));
    }

    private String sqlContaining(String text) {
        return org.mockito.ArgumentMatchers.argThat(sql -> sql != null && sql.contains(text));
    }
}
