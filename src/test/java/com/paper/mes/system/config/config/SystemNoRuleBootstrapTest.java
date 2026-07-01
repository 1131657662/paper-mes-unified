package com.paper.mes.system.config.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemNoRuleBootstrapTest {

    @Test
    void run_whenRuleTextWasCustomized_keepsUserText() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        mockExistingRule(jdbcTemplate, "客户自定义编号", "财务确认后不要覆盖");
        SystemNoRuleBootstrap bootstrap = new SystemNoRuleBootstrap(jdbcTemplate);

        bootstrap.run(null);

        verify(jdbcTemplate, atLeastOnce()).execute(anyString());
        verify(jdbcTemplate, never()).update(sqlContains("UPDATE sys_no_rule"), any(), any(), any());
    }

    @Test
    void run_whenRuleTextIsCorrupted_repairsDefaultText() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        mockExistingRule(jdbcTemplate, "瀹㈡埛缂栫爜", "锟斤拷KH锟斤拷");
        SystemNoRuleBootstrap bootstrap = new SystemNoRuleBootstrap(jdbcTemplate);

        bootstrap.run(null);

        verify(jdbcTemplate).update(sqlContains("UPDATE sys_no_rule"),
                eq("客户编码"),
                eq("默认客户编码：KH+6位全局流水"),
                eq("customer"));
    }

    @SuppressWarnings("unchecked")
    private void mockExistingRule(JdbcTemplate jdbcTemplate, String ruleName, String remark) {
        doAnswer(invocation -> {
            RowMapper<Object> mapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("rule_name")).thenReturn(ruleName);
            when(rs.getString("remark")).thenReturn(remark);
            return List.of(mapper.mapRow(rs, 0));
        }).when(jdbcTemplate).query(sqlContains("SELECT rule_name, remark"), any(RowMapper.class), anyString());
    }

    private String sqlContains(String text) {
        return org.mockito.ArgumentMatchers.argThat(sql -> sql != null && sql.contains(text));
    }
}
