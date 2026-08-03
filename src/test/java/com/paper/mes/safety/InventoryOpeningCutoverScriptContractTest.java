package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryOpeningCutoverScriptContractTest {

    private static final Path SCRIPT = Path.of("deploy/inventory-opening-cutover.example.sh");
    private static final Path POLICY = Path.of("deploy/inventory-opening-policy-support.sh");
    private static final Path EVIDENCE = Path.of("deploy/inventory-opening-evidence-support.sh");
    private static final Path PREVIEW = Path.of("deploy/inventory-opening-preview-verify.mjs");
    private static final Path BEHAVIOR = Path.of("deploy/test-inventory-opening-cutover.sh");

    @Test
    void evidenceScriptIsReadOnlyAndHasBothCutoverChecks() throws Exception {
        String source = source();

        assertThat(source).contains("MODE=\"${MODE:-preflight}\"")
                .contains("run_preflight")
                .contains("run_postcheck")
                .contains("POST /api/inventory/ledger/opening")
                .contains("CUTOVER_MANIFEST_SHA256")
                .contains("write_freeze_confirmed_by")
                .contains("SHA256SUMS")
                .doesNotContain("INSERT INTO biz_inventory_transaction")
                .doesNotContain("UPDATE biz_inventory_transaction")
                .doesNotContain("DELETE FROM biz_inventory_transaction")
                .doesNotContain("DROP DATABASE");
    }

    @Test
    void evidenceScriptRefusesToOverwriteArtifacts() throws Exception {
        String source = source();

        assertThat(source).contains("refusing to overwrite existing preflight evidence")
                .contains("refusing to overwrite existing postcheck evidence");
    }

    @Test
    void evidenceScriptBindsPostcheckToApprovedPreflight() throws Exception {
        String source = source();

        assertThat(source).contains(
                "preflight evidence checksum mismatch",
                "cutover manifest differs from preflight approval",
                "opening occurred_at differs from approved cutover time",
                "opening reason differs from approved cutover reason",
                "cutover reason must match the controlled opening command",
                "non-opening ledger activity detected during cutover",
                "timezone must be Asia/Shanghai");
    }

    @Test
    void behaviorHarnessCoversSuccessTamperingAndConcurrentActivity() throws Exception {
        String source = Files.readString(BEHAVIOR, StandardCharsets.UTF_8);

        assertThat(source).contains(
                "inventory opening cutover behavior test passed",
                "tampered preflight evidence was unexpectedly accepted",
                "tampered opening preview was unexpectedly accepted",
                "opening reason mismatch was unexpectedly accepted",
                "non-opening cutover activity was unexpectedly accepted",
                "FULLY_RESERVED",
                "physical_opening_weight=2.000",
                "sha256sum -c SHA256SUMS");
    }

    @Test
    void previewValidatorRejectsMalformedOrUntrustedResponses() throws Exception {
        String source = Files.readString(PREVIEW, StandardCharsets.UTF_8);

        assertThat(source).contains(
                "MAX_RESPONSE_BYTES = 10 * 1024 * 1024",
                "JSON.parse",
                "response?.code !== 200",
                "response?.message !== \"success\"",
                "data.switchUuid !== switchUuid",
                "data.preview !== true",
                "data.matched !== true");
    }

    @Test
    void previewValidatorComparesEveryRollAndTotalsWithDatabaseProjection() throws Exception {
        String source = Files.readString(PREVIEW, StandardCharsets.UTF_8);

        assertThat(source).contains(
                "projection.has(fields[0])",
                "preview contains a duplicate finish roll",
                "preview contains an unknown finish roll",
                "preview is missing a finish roll",
                "preview projected quantity differs from database projection",
                "preview weight differs from database projection",
                "preview quantity totals differ from database projection",
                "preview weight totals differ from database projection");
    }

    private String source() throws Exception {
        return Files.readString(SCRIPT, StandardCharsets.UTF_8)
                + Files.readString(POLICY, StandardCharsets.UTF_8)
                + Files.readString(EVIDENCE, StandardCharsets.UTF_8);
    }
}
