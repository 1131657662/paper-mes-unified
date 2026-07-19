package com.paper.mes.system.config.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryInventorySchemaBootstrapTest {

    @Test
    void run_whenColumnsAndIndexAreMissing_appliesSchemaAndBackfills() {
        JdbcTemplate jdbcTemplate = jdbcTemplateWithObjectCount(0);

        new DeliveryInventorySchemaBootstrap(jdbcTemplate).run(null);

        verify(jdbcTemplate, atLeastOnce()).execute(any(String.class));
        verify(jdbcTemplate).update(sqlContains("SET f.stock_in_time"));
        verify(jdbcTemplate).update(sqlContains("SET f.warehouse_uuid = o.warehouse_uuid"));
        verify(jdbcTemplate).update(sqlContains("HAVING COUNT(*) = 1"));
        verify(jdbcTemplate).update(sqlContains("COUNT(DISTINCT f.warehouse_uuid) = 1"));
    }

    @Test
    void run_whenColumnsAndIndexExist_onlyRunsSafeBackfills() {
        JdbcTemplate jdbcTemplate = jdbcTemplateWithObjectCount(1);

        new DeliveryInventorySchemaBootstrap(jdbcTemplate).run(null);

        verify(jdbcTemplate, never()).execute(any(String.class));
        verify(jdbcTemplate).update(sqlContains("SET f.stock_in_time"));
        verify(jdbcTemplate).update(sqlContains("SET f.warehouse_uuid = o.warehouse_uuid"));
        verify(jdbcTemplate).update(sqlContains("HAVING COUNT(*) = 1"));
        verify(jdbcTemplate).update(sqlContains("COUNT(DISTINCT f.warehouse_uuid) = 1"));
    }

    private JdbcTemplate jdbcTemplateWithObjectCount(int count) {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class),
                any(String.class), any(String.class))).thenReturn(count);
        return jdbcTemplate;
    }

    private String sqlContains(String text) {
        return org.mockito.ArgumentMatchers.argThat(sql -> sql != null && sql.contains(text));
    }
}
