package com.paper.mes.processorder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessRelationshipIntegritySchemaContractTest {

    @Test
    void baseMigrationAnchorsOriginalStepAndFinishToOneOrderScope() throws IOException {
        String migration = read("sql/V3.55__enforce_process_base_relationships.sql");

        assertThat(migration).contains(
                "V3_55_BASE_RELATIONSHIP_CONFLICTS",
                "uk_original_roll_scope",
                "uk_process_step_stage_scope",
                "uk_finish_roll_scope",
                "fk_original_roll_order",
                "fk_process_step_original_scope",
                "fk_finish_roll_order");
        assertRestrictOnly(migration);
    }

    @Test
    void stageMigrationsRejectCrossScopeOutputsAndInputs() throws IOException {
        String outputMigration = read("sql/V3.56__enforce_process_stage_output_relationships.sql");
        String inputMigration = read("sql/V3.57__enforce_process_stage_input_relationships.sql");

        assertThat(outputMigration).contains(
                "V3_56_STAGE_OUTPUT_RELATIONSHIP_CONFLICTS",
                "uk_stage_output_source_scope",
                "fk_stage_output_original_scope",
                "fk_stage_output_step_scope",
                "fk_stage_output_parent_scope",
                "fk_stage_output_finish_scope");
        assertThat(inputMigration).contains(
                "V3_57_STAGE_INPUT_RELATIONSHIP_CONFLICTS",
                "fk_stage_input_original_scope",
                "fk_stage_input_step_scope",
                "fk_stage_input_output_scope",
                "fk_stage_input_source_output_scope",
                "FOREIGN KEY (`input_output_uuid`, `order_uuid`, `original_uuid`, `source_step_uuid`)");
        assertRestrictOnly(outputMigration + inputMigration);
    }

    @Test
    void lineageMigrationUsesGeneratedColumnsForActivePairUniqueness() throws IOException {
        String migration = read("sql/V3.58__enforce_process_param_and_lineage_relationships.sql");

        assertThat(migration).contains(
                "V3_58_LINEAGE_RELATIONSHIP_CONFLICTS",
                "GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0",
                "uk_finish_original_rel_active",
                "fk_process_param_original_scope",
                "fk_process_param_step_scope",
                "fk_finish_original_rel_finish_scope",
                "fk_finish_original_rel_original_scope");
        assertThat(migration).doesNotContain("UPDATE biz_", "DELETE FROM biz_");
        assertRestrictOnly(migration);
    }

    @Test
    void canonicalSchemaMatchesLatestRelationshipBoundary() throws IOException {
        String schema = read("sql/01_schema_v4.1.sql");
        String version = read("sql/schema-baseline.version").trim();

        assertThat(version).isEqualTo("3.60");
        assertThat(schema).contains(
                "uk_process_step_stage_scope",
                "uk_stage_output_source_scope",
                "fk_stage_output_step_scope",
                "fk_stage_input_source_output_scope",
                "fk_stage_input_output_scope",
                "uk_finish_original_rel_active",
                "fk_finish_original_rel_original_scope");
        int blockStart = schema.indexOf("-- V3.55-V3.58");
        int blockEnd = schema.indexOf("-- 三、出库模块", blockStart);
        assertRestrictOnly(schema.substring(blockStart, blockEnd));
    }

    private void assertRestrictOnly(String sql) {
        assertThat(sql).contains("ON DELETE RESTRICT ON UPDATE RESTRICT");
        assertThat(sql).doesNotContain("ON DELETE CASCADE", "ON UPDATE CASCADE");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
