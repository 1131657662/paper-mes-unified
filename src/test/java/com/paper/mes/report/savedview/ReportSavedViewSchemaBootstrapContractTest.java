package com.paper.mes.report.savedview;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportSavedViewSchemaBootstrapContractTest {
    @Test
    void bootstrap_createsSavedViewTableWhenEnabled() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/paper/mes/report/config/ReportSavedViewSchemaBootstrap.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("ReportSavedViewSchemaSql.createStatements()"));
        assertTrue(source.contains("app.schema-bootstrap"));
    }
}
