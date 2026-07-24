package com.paper.mes.report.subscription;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportSubscriptionSchemaContractTest {

    @Test
    void subscriptionSchema_containsOwnerRecipientAndRunRelationships() throws IOException {
        String schema = source("sql/01_schema_v4.1.sql");

        assertTrue(schema.contains("CREATE TABLE `rpt_report_subscription`"));
        assertTrue(schema.contains("CREATE TABLE `rpt_report_subscription_recipient`"));
        assertTrue(schema.contains("CREATE TABLE `rpt_report_subscription_run`"));
        assertTrue(schema.contains("UNIQUE KEY `uk_report_subscription_run_slot`"));
    }

    @Test
    void subscriptionMigration_supportsIdempotentPeriodPolicyUpgrade() throws IOException {
        String migration = source("sql/V3.20__add_report_subscriptions.sql");

        assertTrue(migration.contains("information_schema.columns"));
        assertTrue(migration.contains("chk_report_subscription_period_policy"));
        assertTrue(migration.contains("uk_report_subscription_owner_active_name"));
    }

    @Test
    void subscriptionRunSchema_allowsFailureBeforeReleaseResolution() throws IOException {
        String schema = source("sql/01_schema_v4.1.sql");
        String migration = source("sql/V3.26__allow_subscription_run_without_release.sql");
        String bootstrap = source("src/main/java/com/paper/mes/report/config/ReportSubscriptionSchemaBootstrap.java");
        String bootstrapSql = source("src/main/java/com/paper/mes/report/config/ReportSubscriptionSchemaSql.java");

        assertTrue(schema.contains("`metric_release_uuid` VARCHAR(36) DEFAULT NULL"));
        assertTrue(migration.contains("MODIFY COLUMN `metric_release_uuid` VARCHAR(36) NULL"));
        assertTrue(bootstrap.contains("MODIFY COLUMN `metric_release_uuid` varchar(36) NULL"));
        assertTrue(bootstrapSql.contains("`metric_release_uuid` varchar(36) DEFAULT NULL"));
    }

    @Test
    void scheduler_usesConnectionScopedNamedLock() throws IOException {
        String scheduler = source("src/main/java/com/paper/mes/report/subscription/config/ReportSubscriptionScheduler.java");

        assertTrue(scheduler.contains("SELECT GET_LOCK(?, 0)"));
        assertTrue(scheduler.contains("SELECT RELEASE_LOCK(?)"));
        assertTrue(scheduler.contains("ConnectionCallback"));
    }

    private String source(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }
}
