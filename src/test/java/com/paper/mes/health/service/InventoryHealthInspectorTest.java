package com.paper.mes.health.service;

import com.paper.mes.health.dto.DataHealthIssueVO;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryHealthInspectorTest {

    @Test
    void inspect_reportsEachOrderWithUnassignedFinishedRolls() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("order_uuid")).thenReturn("order-1");
        when(resultSet.getString("order_no")).thenReturn("JG202607070003");
        when(resultSet.getLong("roll_count")).thenReturn(4L);
        when(resultSet.getBigDecimal("remaining_weight")).thenReturn(BigDecimal.ZERO);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RowMapper<DataHealthIssueVO> mapper = (RowMapper<DataHealthIssueVO>) invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        });

        List<DataHealthIssueVO> issues = new InventoryHealthInspector(jdbcTemplate).inspect();

        assertEquals(1, issues.size());
        assertEquals("UNASSIGNED_FINISH_WAREHOUSE", issues.getFirst().issueType());
        assertEquals("WARNING", issues.getFirst().severity());
        assertEquals("JG202607070003", issues.getFirst().businessNo());
        assertEquals("OPEN_INVENTORY_WAREHOUSE_REPAIR", issues.getFirst().repairAction());
    }
}
