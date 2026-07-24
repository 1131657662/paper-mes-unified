package com.paper.mes.report.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportQuerySnapshotSchemaContractTest {

    @Test
    void freshSchema_enforcesTenSecondIdempotencyKey() throws IOException {
        String migration = source("sql/V3.29__add_report_query_snapshot.sql");

        assertTrue(migration.contains("`idempotency_bucket` BIGINT NOT NULL"));
        assertTrue(migration.contains("UNIQUE KEY `uk_report_query_snapshot_idempotency`"));
        assertTrue(migration.contains("`metric_release_uuid`, `idempotency_bucket`"));
    }

    @Test
    void upgradeMigration_backfillsBeforeAddingConstraint() throws IOException {
        String migration = source("sql/V3.30__add_report_query_snapshot_idempotency.sql");

        int addColumn = migration.indexOf("ADD COLUMN `idempotency_bucket`");
        int backfill = migration.indexOf("FLOOR(UNIX_TIMESTAMP(`create_time`) / 10)");
        int constraint = migration.indexOf("ADD UNIQUE KEY `uk_report_query_snapshot_idempotency`");
        assertTrue(addColumn >= 0 && addColumn < backfill);
        assertTrue(backfill < constraint);
        assertTrue(migration.contains("information_schema.columns"));
        assertTrue(migration.contains("information_schema.statistics"));
        assertTrue(migration.contains("WHERE ranked.duplicate_rank > 1"));
    }

    private String source(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }
}
