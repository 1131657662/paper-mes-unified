package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationGateContractTest {

    @Test
    void migrationExecutor_keepsNamedLockOnPersistentSession() throws Exception {
        String runner = source("deploy/apply-paper-mes-migrations.example.sh");
        String script = source("deploy/migration-lock-support.sh");

        assertThat(runner).contains("migration-lock-support.sh");
        assertThat(script).contains(
                "paper_mes_migration_owner_", "GET_LOCK", "IS_USED_LOCK",
                "lock_owner_check", "SELECT SLEEP(31536000)", "assert_lock_owned");
        assertThat(script).doesNotContain("result=\"$(mysql_query --batch --skip-column-names -e");
    }

    @Test
    void migrationExecutor_recordsFailureWithoutFakingApplied() throws Exception {
        String runner = source("deploy/apply-paper-mes-migrations.example.sh");
        String script = source("deploy/migration-state-support.sh");

        assertThat(runner).contains("migration-state-support.sh");
        assertThat(script).contains(
                "status", "running", "failed", "MIGRATION_RETRY_FAILED",
                "checksum mismatch", "MIGRATION_BASELINE_VERSION");
        assertThat(script).doesNotContain("record_migration");
    }

    @Test
    void migrationConcurrencyHarness_runsTwoProcessesAgainstOneNamedLock() throws Exception {
        String script = source("deploy/test-migration-concurrency.ps1");

        assertThat(script).contains(
                "Start-MigrationRunner", "Wait-ForLockOwner", "IS_USED_LOCK",
                "MIGRATION_LOCK_TIMEOUT_SECONDS='1'", "could not acquire migration lock",
                "migration concurrency guard passed");
    }

    @Test
    void productionSmoke_replaysPendingMigrationsInsteadOfRegisteringAllAsBaseline() throws Exception {
        String script = source("deploy/test-production-profile-startup.ps1");

        assertThat(script).contains("Apply-PendingMigrations", "status='running'", "status='failed'");
        assertThat(script).doesNotContain("Register-MigrationBaseline", "execution_type=\'baseline\'");
    }

    @Test
    void schemaDiffGate_usesIsolatedPrefixedDatabasesAndNormalizedDumps() throws Exception {
        String script = source("deploy/check-paper-mes-schema-diff.example.sh");

        assertThat(script).contains(
                "MIGRATION_BASELINE_VERSION", "paper_mes_schema_diff_",
                "mysqldump", "diff -u", "refusing to overwrite",
                "DROP DATABASE IF EXISTS", "SCHEMA_BASELINE_CHECKSUM",
                "pending migration", "applied|applied");
        assertThat(script).contains("--no-data", "MIGRATION_BASELINE=1");
        assertThat(script).doesNotContain("--triggers=false");
    }

    @Test
    void schemaDiffGate_comparesOnlyApplicationSchemaAndDoesNotClaimHistoricalReplay() throws Exception {
        String script = source("deploy/check-paper-mes-schema-diff.example.sh");

        assertThat(script).contains("--ignore-table=\"${database}.sys_schema_migration\"");
        assertThat(script.split("--ignore-table=", -1)).hasSize(2);
        assertThat(script).contains("compatibility replay", "does not prove a V1-to-current upgrade chain");
    }

    @Test
    void failureRecoveryHarnessVerifiesExplicitRetryAfterFailedMigration() throws Exception {
        String script = source("deploy/test-migration-failure-recovery.ps1");

        assertThat(script).contains(
                "MIGRATION_RETRY_FAILED",
                "failed|applied",
                "migration recovery test database",
                "migration failure recovery guard passed");
    }

    @Test
    void startupMigrationGuardChecksEveryScriptAndHasBehaviorCoverage() throws Exception {
        String guard = source("deploy/verify-paper-mes-migration-state.example.sh");
        String behaviorTest = source("deploy/test-verify-paper-mes-migration-state.sh");

        assertThat(guard).contains(
                "status IS NULL OR status <> 'applied'",
                "is not recorded",
                "checksum does not match the deployed source",
                "sort -z -V");
        assertThat(behaviorTest).contains(
                "non-applied migration",
                "checksum mismatch",
                "missing migration record",
                "migration state guard behavior test passed");
    }

    private String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
