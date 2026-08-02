package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaMigrationGateContractTest {

    @Test
    void canonicalBaselineContainsEveryRuntimeSchemaRequiredByTheLatestBaseline() throws IOException {
        String baseline = read("sql/01_schema_v4.1.sql");

        assertThat(baseline).contains("`active_order_uuid` VARCHAR(36) GENERATED ALWAYS AS");
        assertThat(baseline).contains("UNIQUE KEY `uk_settle_detail_order_active`");
        assertThat(baseline).contains("KEY `idx_finish_unassigned_order`");
        assertThat(baseline).contains("CREATE TABLE `rpt_report_query_snapshot`");
        assertThat(baseline).contains("UNIQUE KEY `uk_report_query_snapshot_idempotency`");
        assertThat(baseline).contains("SHA-256 session token digest");
    }

    @Test
    void migrationRunnerRejectsUnboundedBaselineRegistrationAndTracksFailureState() throws IOException {
        String runner = read("deploy/apply-paper-mes-migrations.example.sh");
        String lockSupport = read("deploy/migration-lock-support.sh");
        String stateSupport = read("deploy/migration-state-support.sh");

        assertThat(runner).contains("MIGRATION_BASELINE_VERSION is required");
        assertThat(runner).contains("MIGRATION_RETRY_FAILED");
        assertThat(lockSupport).contains("GET_LOCK");
        assertThat(stateSupport).contains("status = 'failed'");
        assertThat(stateSupport).contains("status = 'running'");
        assertThat(runner).doesNotContain("record_migration()");
    }

    @Test
    void productionSmokeExecutesPendingMigrationsInsteadOfFabricatingHistory() throws IOException {
        String smoke = read("deploy/test-production-profile-startup.ps1");

        assertThat(smoke).contains("function Apply-PendingMigrations");
        assertThat(smoke).contains("Invoke-MySql $sql $Database");
        assertThat(smoke).contains("status='failed'");
        assertThat(smoke).contains("rpt_report_query_snapshot");
        assertThat(smoke).doesNotContain("function Register-MigrationBaseline");
    }

    @Test
    void formerStandalonePerformanceIndexesAreTrackedByVersionedMigration() throws IOException {
        String migration = read("sql/V3.51__version_p3_indexes.sql");

        assertThat(migration).contains("idx_order_step_sort");
        assertThat(migration).contains("idx_order_row_sort");
        assertThat(migration).contains("idx_cust_status_ctime");
        assertThat(migration).contains("information_schema.statistics");
    }

    @Test
    void powershellSchemaDiffGateIncludesTriggersAndRequiresLedgerImmutability() throws IOException {
        String script = read("deploy/verify-schema-diff.ps1");

        assertThat(script).contains("information_schema.triggers")
                .contains("action_statement")
                .contains("trg_inventory_transaction_no_update")
                .contains("trg_inventory_transaction_no_delete");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
