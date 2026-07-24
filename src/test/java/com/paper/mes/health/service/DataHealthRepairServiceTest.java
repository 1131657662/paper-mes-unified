package com.paper.mes.health.service;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.health.dto.DataHealthRepairRequest;
import com.paper.mes.oplog.service.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataHealthRepairServiceTest {
    private JdbcTemplate jdbcTemplate;
    private DataHealthRepairService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new DataHealthRepairService(jdbcTemplate, mock(OperationLogService.class));
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1")
                .username("repairer").realName("Repairer").build());
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void reconcileSettlement_usesFrozenDetailAmountsAndPreservesSnapshot() throws Exception {
        stubSettlementQueries();
        AtomicReference<String> updateSql = new AtomicReference<>();
        AtomicReference<Object[]> updateArgs = new AtomicReference<>();
        doAnswer(invocation -> {
            updateSql.set(invocation.getArgument(0));
            updateArgs.set((Object[]) invocation.getRawArguments()[1]);
            return 1;
        }).when(jdbcTemplate).update(anyString(), any(Object[].class));

        service.reconcileSettlement("settle-1",
                new DataHealthRepairRequest("reconcile frozen details", "JS-001"));

        assertThat(updateSql.get())
                .contains("service_amount = ?", "WHERE uuid = ?")
                .doesNotContain("snap_bill");
        assertThat(updateArgs.get())
                .containsSequence(new BigDecimal("10"), new BigDecimal("20"),
                        new BigDecimal("30"), new BigDecimal("5"), new BigDecimal("65"),
                        new BigDecimal("35"), new BigDecimal("100"))
                .contains("settle-1");
    }

    private void stubSettlementQueries() throws Exception {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    ResultSet resultSet = resultSetFor(sql);
                    @SuppressWarnings("unchecked")
                    ResultSetExtractor<Object> extractor = invocation.getArgument(1);
                    return extractor.extractData(resultSet);
                });
    }

    private ResultSet resultSetFor(String sql) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        if (sql.contains("FROM biz_settle_order")) {
            when(resultSet.getString("settle_no")).thenReturn("JS-001");
            when(resultSet.getInt("version")).thenReturn(4);
            assertThat(sql).contains("settle_status IN (1, 2, 3)");
            return resultSet;
        }
        if (sql.contains("FROM biz_settle_detail")) {
            when(resultSet.getInt("detail_count")).thenReturn(1);
            decimal(resultSet, "saw_amount", "10");
            decimal(resultSet, "rewind_amount", "20");
            decimal(resultSet, "service_amount", "30");
            decimal(resultSet, "extra_amount", "5");
            decimal(resultSet, "no_tax_amount", "65");
            decimal(resultSet, "tax_amount", "35");
            decimal(resultSet, "total_amount", "100");
            assertThat(sql).doesNotContain("biz_process_order");
            return resultSet;
        }
        decimal(resultSet, "received_amount", "20");
        decimal(resultSet, "cash_amount", "20");
        decimal(resultSet, "scrap_amount", "0");
        decimal(resultSet, "discount_amount", "0");
        return resultSet;
    }

    private void decimal(ResultSet resultSet, String column, String value) throws Exception {
        when(resultSet.getBigDecimal(column)).thenReturn(new BigDecimal(value));
    }
}
