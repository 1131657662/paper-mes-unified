package com.paper.mes.processorder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessStepMainUniquenessSchemaContractTest {
    @Test
    void baseline_enforcesOneActiveMainStepPerOriginalRoll() throws IOException {
        String sql = read("sql/01_schema_v4.1.sql");

        assertTrue(sql.contains("`active_main_original_uuid` VARCHAR(36)"));
        assertTrue(sql.contains("WHEN `is_main` = 1 AND `is_deleted` = 0"));
        assertTrue(sql.contains("UNIQUE KEY `uk_roll_main_step` (`active_main_original_uuid`)"));
    }

    @Test
    void historicalMigration_matchesBaselineConstraint() throws IOException {
        String sql = read("sql/V1.2__add_process_step_unique_index.sql");

        assertTrue(sql.contains("active_main_original_uuid VARCHAR(36)"));
        assertTrue(sql.contains("WHEN is_main = 1 AND is_deleted = 0"));
        assertTrue(sql.contains("UNIQUE KEY uk_roll_main_step"));
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
