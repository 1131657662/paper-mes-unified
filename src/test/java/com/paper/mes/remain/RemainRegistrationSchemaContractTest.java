package com.paper.mes.remain;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RemainRegistrationSchemaContractTest {

    @Test
    void migration_containsOwnershipRegistrationAndLedgerContracts() throws Exception {
        String sql = Files.readString(Path.of("sql/V3.74__add_remain_registration_and_ownership.sql"));

        assertTrue(sql.contains("ALTER TABLE `biz_finish_roll`"));
        assertTrue(sql.contains("ownership_status"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_registration`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_registration_line`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_inventory_lot`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_inventory_ledger`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_remain_line_active_source`"));
        assertTrue(sql.contains("CONSTRAINT `chk_remain_ledger_event`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_price_version`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_application`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_application_line`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_adjustment`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_adjustment_line`"));
        assertTrue(sql.contains("chk_remain_adjustment_line_values"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_customer_credit_account`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_customer_credit_ledger`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_refund`"));
        assertTrue(sql.contains("adjustment_uuid"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_sale`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `biz_remain_sale_line`"));
        assertTrue(sql.contains("SALE_OUT"));
        assertTrue(sql.contains("SALE_REVERSAL"));
        assertTrue(sql.contains("source_type"));
    }
}
