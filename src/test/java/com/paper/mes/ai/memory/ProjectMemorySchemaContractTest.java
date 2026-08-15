package com.paper.mes.ai.memory;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMemorySchemaContractTest {

    @Test
    void checksumIsRegisteredForSpringStartup() {
        assertThat(ProjectMemoryChecksum.class.isAnnotationPresent(Component.class)).isTrue();
    }

    @Test
    void migrationCreatesVersionedJsonSnapshotsWithOneActiveInvariant() throws IOException {
        String migration = read("sql/V3.67__add_ai_project_memory.sql");

        assertThat(migration).contains(
                "CREATE TABLE IF NOT EXISTS `biz_project_memory_doc`",
                "`doc_json` JSON NOT NULL",
                "`active_status` VARCHAR(16) GENERATED ALWAYS",
                "uk_project_memory_doc_version",
                "uk_project_memory_doc_checksum",
                "uk_project_memory_active_status",
                "chk_project_memory_doc_status",
                "V3.67 migration lock not acquired",
                "SELECT RELEASE_LOCK");
    }

    @Test
    void canonicalBaselineContainsTheMemoryTable() throws IOException {
        assertThat(read("sql/01_schema_v4.1.sql"))
                .contains("-- V3.67 canonical baseline", "CREATE TABLE `biz_project_memory_doc`");
    }

    @Test
    void patchAuditMigrationProvidesUniqueIdempotencyAndCanonicalTable() throws IOException {
        assertThat(read("sql/V3.68__add_ai_project_memory_patch_audit.sql"))
                .contains("CREATE TABLE IF NOT EXISTS `biz_project_memory_patch_audit`",
                        "uk_project_memory_patch_idempotency", "operations_json` JSON NOT NULL",
                        "V3.68 migration lock not acquired");
        assertThat(read("sql/01_schema_v4.1.sql"))
                .contains("V3.68 canonical baseline", "CREATE TABLE `biz_project_memory_patch_audit`");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
