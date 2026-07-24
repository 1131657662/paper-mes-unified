package com.paper.mes.report.materialization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportMaterializationSchemaContractTest {
    @Test
    void materializationState_tracksTaskRetryAndPerMetricVersion() throws IOException {
        String migration = source("sql/V3.22__add_report_materialization_runtime.sql");

        assertTrue(migration.contains("`task_id` VARCHAR(64) NOT NULL"));
        assertTrue(migration.contains("`retry_count` INT UNSIGNED NOT NULL DEFAULT 0"));
        assertTrue(migration.contains("FOREIGN KEY (`metric_version_uuid`, `metric_uuid`)"));
        assertTrue(migration.contains("`active_generation_uuid` VARCHAR(36)"));
    }

    @Test
    void metricValue_isQuarterPartitionedAndCarriesVersionPerValue() throws IOException {
        String migration = source("sql/V3.23__add_report_metric_values_and_snapshots.sql");
        String valueTable = migration.substring(migration.indexOf("CREATE TABLE IF NOT EXISTS `rpt_metric_value` ("),
                migration.indexOf("CREATE TABLE IF NOT EXISTS `rpt_report_snapshot`"));

        assertTrue(valueTable.contains("PARTITION BY RANGE COLUMNS (`period_start`)"));
        assertTrue(valueTable.contains("`metric_version_uuid` VARCHAR(36) NOT NULL"));
        assertTrue(valueTable.contains("`dimension_set_code` VARCHAR(64) NOT NULL"));
        assertFalse(valueTable.contains("FOREIGN KEY"));
    }

    @Test
    void snapshotCleanup_preservesReferencedSnapshots() throws IOException {
        String service = source("src/main/java/com/paper/mes/report/materialization/service/ReportSnapshotCleanupService.java");

        assertTrue(service.contains("NOT EXISTS"));
        assertTrue(service.contains("rpt_report_snapshot_reference"));
        assertTrue(service.contains("r.release_status = 3"));
    }

    @Test
    void publication_withoutLongTableForeignKeys_validatesReleaseItemBinding() throws IOException {
        String service = source("src/main/java/com/paper/mes/report/materialization/service/ReportMetricPublicationService.java");

        assertTrue(service.contains("LEFT JOIN rpt_metric_release_item"));
        assertTrue(service.contains("item.metric_version_uuid = s.metric_version_uuid"));
        assertTrue(service.contains("s.metric_release_uuid <> job.metric_release_uuid"));
    }

    private String source(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }
}
