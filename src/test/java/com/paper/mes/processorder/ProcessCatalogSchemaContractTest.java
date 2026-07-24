package com.paper.mes.processorder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessCatalogSchemaContractTest {

    @Test
    void migrationDefinesNormalizedCatalogAndCompatibilityForeignKey() throws IOException {
        String sql = read("sql/V3.37__add_process_catalog.sql");

        assertTrue(sql.contains("sys_process_catalog`"));
        assertTrue(sql.contains("sys_process_catalog_unit`"));
        assertTrue(sql.contains("sys_process_catalog_billing_mode`"));
        assertTrue(sql.contains("fk_process_step_catalog_type"));
        assertTrue(sql.contains("DROP CHECK `chk_process_step_type`"));
        assertTrue(sql.contains("DROP CHECK `chk_process_step_service_basis`"));
    }

    @Test
    void migrationSeedsCompatibleProcessesAndExplicitStrategies() throws IOException {
        String sql = read("sql/V3.37__add_process_catalog.sql");

        assertTrue(sql.contains("'SAW_KNIFE'"));
        assertTrue(sql.contains("'REWIND_WEIGHT'"));
        assertTrue(sql.contains("'SERVICE_QUANTITY'"));
        assertTrue(sql.contains("'process-catalog-repack',4"));
        assertFalse(sql.contains("CHECK (`step_type` IN (1,2,3,4))"));
    }

    @Test
    void baselineContainsCatalogBeforeProcessStep() throws IOException {
        String sql = read("sql/01_schema_v4.1.sql");

        int catalog = sql.indexOf("CREATE TABLE `sys_process_catalog`");
        int processStep = sql.indexOf("CREATE TABLE `biz_process_step`");
        assertTrue(catalog >= 0 && catalog < processStep);
        assertTrue(sql.contains("fk_process_step_catalog_type"));
        assertFalse(sql.contains("chk_process_step_service_basis"));
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
