package com.paper.mes.report;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportIndexContractTest {

    @Test
    void schema_whenCreatingOriginalRoll_keepsMachineUuidIndex() throws IOException {
        String schema = source("sql/01_schema_v4.1.sql");
        String originalRollTable = slice(schema,
                "CREATE TABLE `biz_original_roll`", "ENGINE=InnoDB");

        assertTrue(originalRollTable.contains("KEY `idx_original_roll_machine_uuid` (`machine_uuid`)"));
    }

    @Test
    void bootstrap_whenExistingDatabaseStarts_addsMachineUuidIndex() throws IOException {
        String source = source(
                "src/main/java/com/paper/mes/system/config/config/ReportIntegrityBootstrap.java");

        assertTrue(source.contains("addOriginalRollMachineIndex();"));
        assertTrue(source.contains("information_schema.statistics"));
        assertTrue(source.contains("ADD INDEX `idx_original_roll_machine_uuid` (`machine_uuid`)"));
    }

    private String source(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }

    private String slice(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        int endIndex = text.indexOf(end, startIndex + start.length());
        assertTrue(startIndex >= 0, "Missing start: " + start);
        assertTrue(endIndex >= 0, "Missing end: " + end);
        return text.substring(startIndex, endIndex);
    }
}
