package com.paper.mes.health.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettlementHealthInspectorTest {

    @Test
    void inspect_excludesVoidedSettlementsFromAmountMismatchCheck() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());
        SettlementHealthInspector inspector = new SettlementHealthInspector(jdbcTemplate, new ObjectMapper());

        inspector.inspect();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(3)).query(sqlCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getAllValues().getFirst())
                .contains("s.settle_status IN (1, 2, 3)");
    }
}
