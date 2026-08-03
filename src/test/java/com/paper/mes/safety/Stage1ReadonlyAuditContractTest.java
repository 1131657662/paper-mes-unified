package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class Stage1ReadonlyAuditContractTest {

    private static final Path AUDIT = Path.of("deploy/stage1-readonly-audit.example.sh");
    private static final Path BEHAVIOR = Path.of("deploy/test-stage1-readonly-audit.sh");

    @Test
    void auditCoversStageLineageAndDocumentRelationshipGates() throws Exception {
        String source = source(AUDIT);

        assertThat(source).contains(
                "orphan_stage_output_parent", "stage_input_cross_order",
                "orphan_process_param_step", "duplicate_active_finish_original_pair",
                "delivery_detail_cross_order", "orphan_settle_detail_settle");
        assertThat(source).contains(
                "snap_print_without_applied_issue_version",
                "processing_print_count_zero", "soft_delete_process_config_risk");
    }

    @Test
    void auditContainsNoDatabaseMutationStatements() throws Exception {
        String source = source(AUDIT).toUpperCase();

        assertThat(source).doesNotContain(
                "INSERT ", "UPDATE ", "DELETE ", "DROP ",
                "ALTER ", "TRUNCATE ", "CREATE ", "REPLACE ", "CALL ");
    }

    @Test
    void behaviorFixtureCoversHealthyAndConflictResults() throws Exception {
        String source = source(BEHAVIOR);

        assertThat(source).contains(
                "stage 1 read-only audit passed",
                "STAGE1_FAIL_CHECK=orphan_stage_input_output",
                "unexpectedly accepted an orphan stage input");
        assertThat(source).contains("contains a mutation statement");
    }

    private String source(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
