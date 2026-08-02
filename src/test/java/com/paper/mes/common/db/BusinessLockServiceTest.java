package com.paper.mes.common.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessLockServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        if (TransactionSynchronizationManager.hasResource("paper_mes_inventory_switch")) {
            TransactionSynchronizationManager.unbindResource("paper_mes_inventory_switch");
        }
    }

    @Test
    void inventorySwitchLock_releasesOnlyAfterTransactionCompletion() {
        when(jdbcTemplate.queryForObject(eq("SELECT GET_LOCK(?, 30)"), eq(Integer.class),
                eq("paper_mes_inventory_switch"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(eq("SELECT RELEASE_LOCK(?)"), eq(Integer.class),
                eq("paper_mes_inventory_switch"))).thenReturn(1);
        TransactionSynchronizationManager.initSynchronization();

        new BusinessLockService(jdbcTemplate).lockInventorySwitch();

        verify(jdbcTemplate, never()).queryForObject(eq("SELECT RELEASE_LOCK(?)"), eq(Integer.class),
                eq("paper_mes_inventory_switch"));
        TransactionSynchronization synchronization = TransactionSynchronizationManager
                .getSynchronizations().getFirst();
        synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        verify(jdbcTemplate).queryForObject(eq("SELECT RELEASE_LOCK(?)"), eq(Integer.class),
                eq("paper_mes_inventory_switch"));
    }

    @Test
    void inventorySwitchLock_requiresTransactionSynchronization() {
        assertThatThrownBy(() -> new BusinessLockService(jdbcTemplate).lockInventorySwitch())
                .hasMessageContaining("active transaction");
    }
}
