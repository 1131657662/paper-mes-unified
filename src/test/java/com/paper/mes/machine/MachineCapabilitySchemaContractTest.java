package com.paper.mes.machine;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineCapabilitySchemaContractTest {

    @Test
    void migrationDefinesNormalizedCapabilitiesAndPerProcessDefault() throws IOException {
        String sql = read("sql/V3.41__add_machine_process_capabilities.sql");

        assertTrue(sql.contains("sys_machine_process_capability"));
        assertTrue(sql.contains("uk_machine_capability_active"));
        assertTrue(sql.contains("uk_machine_capability_default"));
        assertTrue(sql.contains("fk_machine_capability_catalog"));
        assertTrue(sql.contains("process_code IN ('SAW','REWIND')"));
    }

    @Test
    void baselineCreatesCapabilityAfterReferencedTables() throws IOException {
        String sql = read("sql/01_schema_v4.1.sql");

        int machine = sql.indexOf("CREATE TABLE `sys_machine`");
        int catalog = sql.indexOf("CREATE TABLE `sys_process_catalog`");
        int capability = sql.indexOf("CREATE TABLE `sys_machine_process_capability`");
        assertTrue(machine >= 0 && catalog > machine && capability > catalog);
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
