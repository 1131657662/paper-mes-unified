package com.paper.mes.system.config.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SystemConfigBootstrapMigrationTest {

    @Test
    void migrateDeliveryCashConfigLabel_updatesOnlyBuiltInActiveRowMetadata() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SystemConfigBootstrap bootstrap = new SystemConfigBootstrap(jdbcTemplate);

        bootstrap.migrateDeliveryCashConfigLabel();

        verify(jdbcTemplate).update(
                eq("UPDATE sys_config_item SET config_name=?, remark=?, update_by=?"
                        + " WHERE config_key=? AND built_in=1 AND is_deleted=0"),
                eq("现结出库拦截模式"),
                eq("0关闭拦截，1警告放行，2强制拦截"),
                eq("system"),
                eq("delivery.cashSettleBlockMode"));
    }

    @Test
    void migrateSettlementDiscountConfigLabels_updatesThresholdSemantics() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SystemConfigBootstrap bootstrap = new SystemConfigBootstrap(jdbcTemplate);

        bootstrap.migrateSettlementDiscountConfigLabels();

        verify(jdbcTemplate).update(
                eq("UPDATE sys_config_item SET config_name=?, remark=?, update_by=?"
                        + " WHERE config_key=? AND built_in=1 AND is_deleted=0"),
                eq("优惠免审金额阈值"),
                eq("不超过该金额的优惠可由有权限财务直接核销"),
                eq("system"),
                eq("settle.discountAutoApproveLimit"));
        verify(jdbcTemplate).update(
                eq("UPDATE sys_config_item SET config_name=?, remark=?, update_by=?"
                        + " WHERE config_key=? AND built_in=1 AND is_deleted=0"),
                eq("财务复核金额上限"),
                eq("超过该金额时升级为管理员审批"),
                eq("system"),
                eq("settle.discountMaxAmount"));
        verify(jdbcTemplate).update(
                eq("UPDATE sys_config_item SET config_name=?, remark=?, update_by=?"
                        + " WHERE config_key=? AND built_in=1 AND is_deleted=0"),
                eq("财务复核比例上限"),
                eq("超过未收金额的该比例时升级为管理员审批"),
                eq("system"),
                eq("settle.discountMaxPercent"));
    }
}
