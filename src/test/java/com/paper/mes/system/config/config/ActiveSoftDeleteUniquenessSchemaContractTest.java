package com.paper.mes.system.config.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveSoftDeleteUniquenessSchemaContractTest {

    @Test
    void migrationRejectsConflictsBeforeReplacingLegacyIndexes() throws IOException {
        String migration = read("sql/V3.59__enforce_active_soft_delete_uniqueness.sql");

        assertThat(migration).contains(
                "paper_mes_active_soft_delete_uniqueness",
                "V3_59_MIGRATION_LOCK_NOT_ACQUIRED",
                "V3_59_ACTIVE_SOFT_DELETE_CONFLICTS",
                "GROUP BY order_uuid, original_uuid HAVING COUNT(*) > 1",
                "GROUP BY dict_type, item_code HAVING COUNT(*) > 1",
                "GROUP BY config_key HAVING COUNT(*) > 1",
                "GROUP BY biz_type HAVING COUNT(*) > 1");
        assertThat(migration).doesNotContain(
                "UPDATE `biz_process_config_draft`",
                "UPDATE `sys_dict_item`",
                "UPDATE `sys_config_item`",
                "UPDATE `sys_no_rule`",
                "DELETE FROM");
    }

    @Test
    void migrationAndCanonicalSchemaUseNullableActiveKeys() throws IOException {
        String migration = read("sql/V3.59__enforce_active_soft_delete_uniqueness.sql");
        String schema = read("sql/01_schema_v4.1.sql");

        assertActiveKeyContract(migration);
        assertActiveKeyContract(schema);
        assertThat(migration).contains(
                "DROP INDEX `uk_config_draft_roll`",
                "DROP INDEX `uk_sys_dict_item_code`",
                "DROP INDEX `uk_sys_config_key`",
                "DROP INDEX `uk_sys_no_rule_biz`");
        assertThat(schema).doesNotContain(
                "UNIQUE KEY `uk_config_draft_roll` (",
                "UNIQUE KEY `uk_sys_dict_item_code` (",
                "UNIQUE KEY `uk_sys_config_key` (",
                "UNIQUE KEY `uk_sys_no_rule_biz` (");
        assertThat(read("sql/schema-baseline.version").trim()).isEqualTo("3.73.1");
    }

    @Test
    void runtimeBootstrapsCreateTheSameActiveKeyIndexes() throws IOException {
        String draft = read("src/main/java/com/paper/mes/system/config/config/ProcessDraftIntegrityBootstrap.java");
        String config = read("src/main/java/com/paper/mes/system/config/config/SystemConfigBootstrap.java");
        String noRule = read("src/main/java/com/paper/mes/system/config/config/SystemNoRuleBootstrap.java");

        assertThat(draft).contains("active_order_uuid", "active_original_uuid", "uk_config_draft_roll_active")
                .doesNotContain("UNIQUE KEY `uk_config_draft_roll` (");
        assertThat(config).contains("active_dict_type", "active_item_code", "active_config_key",
                        "uk_sys_dict_item_code_active", "uk_sys_config_key_active")
                .doesNotContain("UNIQUE KEY `uk_sys_dict_item_code` (", "UNIQUE KEY `uk_sys_config_key` (");
        assertThat(noRule).contains("active_biz_type", "uk_sys_no_rule_biz_active")
                .doesNotContain("UNIQUE KEY `uk_sys_no_rule_biz` (");
    }

    private void assertActiveKeyContract(String sql) {
        assertThat(sql).contains(
                "CASE WHEN `is_deleted` = 0 THEN `order_uuid` ELSE NULL END",
                "CASE WHEN `is_deleted` = 0 THEN `original_uuid` ELSE NULL END",
                "CASE WHEN `is_deleted` = 0 THEN `dict_type` ELSE NULL END",
                "CASE WHEN `is_deleted` = 0 THEN `item_code` ELSE NULL END",
                "CASE WHEN `is_deleted` = 0 THEN `config_key` ELSE NULL END",
                "CASE WHEN `is_deleted` = 0 THEN `biz_type` ELSE NULL END",
                "uk_config_draft_roll_active",
                "uk_sys_dict_item_code_active",
                "uk_sys_config_key_active",
                "uk_sys_no_rule_biz_active");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
