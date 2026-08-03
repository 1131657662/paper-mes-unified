package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricalSchemaFixtureGateContractTest {

    private static final Path SCRIPT = Path.of(
            "deploy/verify-historical-schema-fixture.example.sh");
    private static final Path SUPPORT = Path.of(
            "deploy/historical-fixture-support.sh");
    private static final Path BEHAVIOR = Path.of(
            "deploy/test-historical-fixture-support.sh");

    @Test
    void fixtureGateRequiresExplicitBusinessApprovalAndChecksums() throws Exception {
        String source = source();

        assertThat(source).contains(
                "paper-mes-historical-fixture-v1",
                "fixture_sha256",
                "FIXTURE_MANIFEST_SHA256",
                "sanitization",
                "approved_by",
                "approved_at",
                "fixture checksum mismatch",
                "fixture manifest checksum mismatch",
                "canonical schema is not a historical fixture",
                "stage_artifacts",
                "manifest_sha256");
    }

    @Test
    void fixtureGateReplaysOnlyMigrationsNewerThanAttestedVersion() throws Exception {
        String source = source();

        assertThat(source).contains(
                "is_newer_version",
                "prepare_pending_migrations",
                "fixture_version is not a known migration boundary",
                "MIGRATION_BASELINE=0",
                "sys_schema_migration_fixture_gate",
                "migrated fixture schema differs from canonical schema");
        assertThat(source).doesNotContain("MIGRATION_BASELINE=1");
    }

    @Test
    void fixtureGateRestrictsDumpAndDisposableDatabaseScope() throws Exception {
        String source = source();

        assertThat(source).contains(
                "^paper_mes_history_[a-z0-9_]+$",
                "refusing to overwrite",
                "CREATE|DROP",
                "GRANT|REVOKE",
                "INTO[[:space:]]+OUTFILE",
                "fixture is missing required historical tables",
                "DROP DATABASE IF EXISTS");
    }

    @Test
    void behaviorHarnessCoversVersionSelectionAndScopeRejection() throws Exception {
        String source = Files.readString(BEHAVIOR, StandardCharsets.UTF_8);

        assertThat(source).contains(
                "historical fixture support behavior test passed",
                "unknown migration boundary was unexpectedly accepted",
                "out-of-scope dump was unexpectedly accepted",
                "V2.0__third.sql");
    }

    private String source() throws Exception {
        return Files.readString(SCRIPT, StandardCharsets.UTF_8)
                + Files.readString(SUPPORT, StandardCharsets.UTF_8);
    }
}
