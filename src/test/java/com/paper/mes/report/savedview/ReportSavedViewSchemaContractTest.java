package com.paper.mes.report.savedview;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportSavedViewSchemaContractTest {
    @Test
    void migration_enforcesOwnerNameAndDefaultUniqueness() throws IOException {
        String sql = source("sql/V3.33__add_report_saved_views.sql");

        assertTrue(sql.contains("uk_report_saved_view_owner_name"));
        assertTrue(sql.contains("uk_report_saved_view_owner_default"));
        assertTrue(sql.contains("fk_report_saved_view_owner"));
    }

    @Test
    void baseline_containsSavedViewTableForFreshInstallations() throws IOException {
        assertTrue(source("sql/01_schema_v4.1.sql").contains("CREATE TABLE `rpt_report_saved_view`"));
    }

    private String source(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
