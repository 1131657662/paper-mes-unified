package com.paper.mes.customer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerProcessPriceSchemaContractTest {

    @Test
    void migrationConstrainsPriceOptionsAndSingleDefault() throws IOException {
        String sql = read("sql/V3.42__add_customer_process_prices.sql");

        assertTrue(sql.contains("sys_customer_process_price"));
        assertTrue(sql.contains("uk_customer_process_price_active"));
        assertTrue(sql.contains("uk_customer_process_price_default"));
        assertTrue(sql.contains("'PIECE','TON','FIXED'"));
        assertTrue(sql.contains("fk_customer_process_price_customer"));
        assertTrue(sql.contains("fk_customer_process_price_catalog"));
    }

    @Test
    void baselineContainsCustomerProcessPricesAfterDependencies() throws IOException {
        String sql = read("sql/01_schema_v4.1.sql");
        int customer = sql.indexOf("CREATE TABLE `sys_customer`");
        int catalog = sql.indexOf("CREATE TABLE `sys_process_catalog`");
        int prices = sql.indexOf("CREATE TABLE `sys_customer_process_price`");

        assertTrue(customer >= 0 && customer < prices);
        assertTrue(catalog >= 0 && catalog < prices);
        assertTrue(sql.contains("uk_customer_process_price_default"));
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
