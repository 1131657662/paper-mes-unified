package com.paper.mes.processorder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceProcessStepSchemaContractTest {

    @Test
    void migration_isIdempotentAndConstrainsServiceFields() throws IOException {
        String sql = read("sql/V3.36__add_service_process_steps.sql");

        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(sql.contains("billing_basis"));
        assertTrue(sql.contains("service_quantity"));
        assertTrue(sql.contains("service_amount"));
        assertTrue(sql.contains("chk_process_step_type"));
    }

    @Test
    void baseline_containsServiceProcessFields() throws IOException {
        String sql = read("sql/01_schema_v4.1.sql");

        assertTrue(sql.contains("billing_basis"));
        assertTrue(sql.contains("service_quantity"));
        assertTrue(sql.contains("service_amount"));
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
