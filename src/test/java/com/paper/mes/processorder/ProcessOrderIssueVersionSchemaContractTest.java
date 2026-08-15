package com.paper.mes.processorder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderIssueVersionSchemaContractTest {

    @Test
    void migrationRetainsBeforeAfterSnapshotsAndGuardsVersionUniqueness() throws IOException {
        String migration = Files.readString(
                Path.of("sql/V3.53__add_process_order_issue_versions.sql"), StandardCharsets.UTF_8);

        assertThat(migration).contains(
                "CREATE TABLE IF NOT EXISTS `biz_process_order_issue_version`",
                "`snapshot_before`      LONGTEXT",
                "`snapshot_after`       LONGTEXT",
                "UNIQUE KEY `uk_process_order_issue_version` (`order_uuid`, `version_no`)",
                "FOREIGN KEY (`order_uuid`) REFERENCES `biz_process_order` (`uuid`)",
                "'PENDING','APPLIED','ARCHIVED'",
                "V3_53_MIGRATION_LOCK_NOT_ACQUIRED");
        assertThat(migration).doesNotContain("UPDATE `biz_process_order`");
    }

    @Test
    void canonicalBaselineIncludesIssueVersionTableAndCurrentVersion() throws IOException {
        String schema = Files.readString(
                Path.of("sql/01_schema_v4.1.sql"), StandardCharsets.UTF_8);
        String baselineVersion = Files.readString(
                Path.of("sql/schema-baseline.version"), StandardCharsets.UTF_8).trim();

        assertThat(baselineVersion).isEqualTo("3.68");
        assertThat(schema).contains(
                "CREATE TABLE `biz_process_order_issue_version`",
                "UNIQUE KEY `uk_process_order_issue_version` (`order_uuid`, `version_no`)",
                "UNIQUE KEY `uk_process_order_issue_request` (`order_uuid`, `request_id`)",
                "`request_id`           VARCHAR(64)",
                "`payload_hash`         CHAR(64)",
                "CONSTRAINT `fk_process_order_issue_version_order`");
        assertThat(schema).contains(
                "trg_process_order_issue_version_no_terminal_update",
                "trg_process_order_issue_version_no_terminal_delete");
    }

    @Test
    void v354AddsRequestFingerprintAndTerminalStateGuards() throws IOException {
        String migration = Files.readString(
                Path.of("sql/V3.54__protect_issue_versions_and_add_reissue_idempotency.sql"),
                StandardCharsets.UTF_8);

        assertThat(migration).contains(
                "`request_id` VARCHAR(64)",
                "`payload_hash` CHAR(64)",
                "uk_process_order_issue_request",
                "trg_process_order_issue_version_no_terminal_update",
                "trg_process_order_issue_version_no_terminal_delete",
                "OLD.`status` IN ('APPLIED', 'ARCHIVED')",
                "V3_54_MIGRATION_LOCK_NOT_ACQUIRED");
    }

    @Test
    void v364SeparatesPostProductionNotesFromIssuedProductionSnapshots() throws IOException {
        String migration = Files.readString(
                Path.of("sql/V3.64__add_process_order_post_production_note.sql"), StandardCharsets.UTF_8);
        String schema = Files.readString(
                Path.of("sql/01_schema_v4.1.sql"), StandardCharsets.UTF_8);

        assertThat(migration).contains(
                "paper_mes_process_order_post_production_note",
                "V3_64_MIGRATION_LOCK_NOT_ACQUIRED",
                "post_production_note",
                "AFTER `remark_long`");
        assertThat(schema).contains(
                "`post_production_note`  TEXT          DEFAULT NULL",
                "后生产运营备注，不进入下发快照");
    }
}
