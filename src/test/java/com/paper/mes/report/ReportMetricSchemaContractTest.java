package com.paper.mes.report;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportMetricSchemaContractTest {

    @Test
    void metricSchema_whenCreatingFreshDatabase_containsVersionedReleaseTables() throws IOException {
        String schema = source("sql/01_schema_v4.1.sql");

        assertTrue(schema.contains("CREATE TABLE `rpt_metric_definition`"));
        assertTrue(schema.contains("CREATE TABLE `rpt_metric_version`"));
        assertTrue(schema.contains("CREATE TABLE `rpt_metric_release`"));
        assertTrue(schema.contains("CREATE TABLE `rpt_metric_release_item`"));
    }

    @Test
    void metricRelease_whenPublished_enforcesSingleActiveSlot() throws IOException {
        String migration = source("sql/V3.19__add_report_metric_semantic_release.sql");

        assertTrue(migration.contains("CASE WHEN `release_status` = 2 AND `is_deleted` = 0 THEN 1 ELSE NULL END"));
        assertTrue(migration.contains("UNIQUE KEY `uk_metric_release_active` (`active_slot`)"));
    }

    @Test
    void releaseItem_whenBindingVersion_enforcesVersionBelongsToMetric() throws IOException {
        String migration = source("sql/V3.19__add_report_metric_semantic_release.sql");

        assertTrue(migration.contains("UNIQUE KEY `uk_metric_version_identity` (`uuid`, `metric_uuid`)"));
        assertTrue(migration.contains("FOREIGN KEY (`metric_version_uuid`, `metric_uuid`)"));
        assertTrue(migration.contains("REFERENCES `rpt_metric_version` (`uuid`, `metric_uuid`)"));
    }

    @Test
    void baselineRelease_whenSeeded_containsEveryCurrentAtomicMetric() throws IOException {
        String migration = source("sql/V3.19__add_report_metric_semantic_release.sql");
        String seed = slice(migration, "INSERT INTO `rpt_metric_definition`", "ON DUPLICATE KEY UPDATE");

        assertEquals(19, count(seed, "REPLACE(UUID(), '-', '')"));
        assertTrue(seed.contains("'received_amount', '已收金额'"));
        assertTrue(seed.contains("'unreceived_amount', '已结算未收'"));
    }

    @Test
    void metricBootstrap_whenMultipleInstancesStart_usesDatabaseLockAndIdempotentWrites() throws IOException {
        String seeder = source("src/main/java/com/paper/mes/report/config/ReportMetricBaselineSeeder.java");

        assertTrue(seeder.contains("SELECT GET_LOCK(?, 10)"));
        assertTrue(seeder.contains("SELECT RELEASE_LOCK(?)"));
        assertTrue(seeder.contains("SET SESSION group_concat_max_len = 65535"));
        assertTrue(seeder.contains("INSERT IGNORE INTO rpt_metric_definition"));
        assertTrue(seeder.contains("INSERT IGNORE INTO rpt_metric_version"));
        assertTrue(seeder.contains("if (hasPublishedCurrentRelease()) return;"));
        assertTrue(seeder.contains("release_code = 'REPORT-BASELINE-V2'"));
        assertTrue(seeder.contains("retirePreviousRelease();"));
    }

    @Test
    void operationalMetricRelease_isPublishedAsNewImmutableBundle() throws IOException {
        String migration = source("sql/V3.31__publish_report_metric_baseline_v2.sql");

        assertTrue(migration.contains("'REPORT-BASELINE-V2'"));
        assertTrue(migration.contains("'inventory_locked_weight_kg'"));
        assertTrue(migration.contains("'delivery_completed_weight_kg'"));
        assertTrue(migration.contains("release_status = 3"));
        assertTrue(migration.contains("START TRANSACTION"));
        assertTrue(migration.contains("COMMIT"));
    }

    @Test
    void metricCatalog_whenListingReleases_preAggregatesItemsAndParameterizesDetail() throws IOException {
        String service = source("src/main/java/com/paper/mes/report/service/ReportMetricCatalogService.java");

        assertTrue(service.contains("SELECT release_uuid, COUNT(*) AS metric_count"));
        assertTrue(service.contains("c ON c.release_uuid = r.uuid"));
        assertTrue(service.contains("WHERE r.uuid = ? AND r.is_deleted = 0"));
        assertTrue(service.contains("v.metric_uuid = i.metric_uuid"));
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

    private long count(String text, String pattern) {
        return (text.length() - text.replace(pattern, "").length()) / pattern.length();
    }
}
