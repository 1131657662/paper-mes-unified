package com.paper.mes.processorder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessRollDispositionSchemaContractTest {

    @Test
    void migrationDefinesAuditableDispositionWithIdempotencyAndRestrictiveReferences() throws IOException {
        String migration = read("sql/V3.66__add_process_roll_disposition.sql");

        assertThat(migration).contains(
                "CREATE TABLE IF NOT EXISTS `biz_process_roll_disposition`",
                "disposition_action VARCHAR(32)",
                "target_finish_uuids` JSON",
                "idx_original_roll_disposition",
                "uk_process_roll_disposition_source",
                "uk_process_roll_disposition_request",
                "idx_process_roll_disposition_order",
                "fk_process_roll_disposition_target_order",
                "fk_process_roll_disposition_target_roll",
                "fk_process_roll_disposition_target_finish",
                "chk_process_roll_disposition_action",
                "chk_process_roll_disposition_status",
                "V3.66 migration lock not acquired",
                "ON DELETE RESTRICT ON UPDATE RESTRICT");
    }

    @Test
    void canonicalSchemaContainsTheSameDispositionBoundary() throws IOException {
        String schema = read("sql/01_schema_v4.1.sql");

        assertThat(schema).contains(
                "-- V3.66 canonical baseline",
                "CREATE TABLE `biz_process_roll_disposition`",
                "disposition_action` VARCHAR(32)",
                "target_finish_uuids` JSON",
                "idx_original_roll_disposition",
                "uk_process_roll_disposition_source",
                "uk_process_roll_disposition_request",
                "chk_process_roll_disposition_reason");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
