package com.paper.mes.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaReadinessServiceTest {

    @Test
    void reportsReadyWhenAllCriticalStructuresExist() {
        SchemaReadinessService service = service(new ReadinessJdbcTemplate(null));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isTrue();
        assertThat(report.databaseVersion()).isEqualTo("3.76");
        assertThat(report.missingStructures()).isEmpty();
    }

    @Test
    void reportsTheExactMissingStructure() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("active_order_uuid"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures())
                .containsExactly("column:biz_process_order_append_session.active_order_uuid");
    }

    @Test
    void reportsTheExactMissingConstraint() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("chk_discount_approval_components"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures()).containsExactly(
                "constraint:biz_settle_discount_approval.chk_discount_approval_components");
    }

    @Test
    void reportsMissingDispositionIndex() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("idx_original_roll_disposition"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures()).containsExactly(
                "index:biz_original_roll.idx_original_roll_disposition");
    }

    @Test
    void reportsMissingDispositionSourceUniquenessIndex() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("uk_process_roll_disposition_source"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures()).containsExactly(
                "index:biz_process_roll_disposition.uk_process_roll_disposition_source");
    }

    @Test
    void reportsMissingDispositionRequestUniquenessIndex() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("uk_process_roll_disposition_request"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures()).containsExactly(
                "index:biz_process_roll_disposition.uk_process_roll_disposition_request");
    }

    @Test
    void reportsMissingDispositionTargetFinishUuidColumn() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("target_finish_uuids"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures()).containsExactly(
                "column:biz_process_roll_disposition.target_finish_uuids");
    }

    @Test
    void reportsMissingAiAuditAttemptIndex() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("idx_ai_audit_attempt"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures()).containsExactly(
                "index:sys_ai_call_audit.idx_ai_audit_attempt");
    }

    @Test
    void reportsMissingAiDialoguePreviewHashColumn() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("preview_hash"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures()).containsExactly(
                "column:biz_process_ai_parse.preview_hash");
    }

    @Test
    void reportsMissingGenerationScopedAiMessageIdempotencyIndex() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("uk_ai_message_idempotency_generation"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures()).containsExactly(
                "index:biz_process_ai_message.uk_ai_message_idempotency_generation",
                "index-columns:biz_process_ai_message.uk_ai_message_idempotency_generation="
                        + "conversation_id,memory_generation,idempotency_key");
    }

    @Test
    void reportsMissingProjectMemoryEvidenceParseConstraint() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("fk_memory_evidence_parse"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures()).containsExactly(
                "constraint:biz_project_memory_candidate_evidence.fk_memory_evidence_parse",
                "foreign-key-delete-rule:biz_project_memory_candidate_evidence.fk_memory_evidence_parse=SET NULL");
    }

    @Test
    void reportsEvidenceReferenceColumnThatIsStillNotNullable() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("order_uuid"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures()).containsExactly(
                "column-nullable:biz_project_memory_candidate_evidence.order_uuid");
    }

    @Test
    void reportsEvidenceUniquenessIndexWithTheWrongColumns() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("candidate_uuid,order_ref_hash"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures()).containsExactly(
                "index-columns:biz_project_memory_candidate_evidence.uk_memory_candidate_order_ref=candidate_uuid,order_ref_hash");
    }

    @Test
    void reportsEvidenceForeignKeyWithTheWrongDeleteAction() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("SET NULL"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures()).containsExactly(
                "foreign-key-delete-rule:biz_project_memory_candidate_evidence.fk_memory_evidence_order=SET NULL",
                "foreign-key-delete-rule:biz_project_memory_candidate_evidence.fk_memory_evidence_parse=SET NULL");
    }

    @Test
    void selectsTheHighestSemanticMigrationVersion() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate(null, List.of("3.61", "3.9", "3.63")));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.databaseVersion()).isEqualTo("3.63");
        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures())
                .contains("migration:expected=3.76,actual=3.63");
    }

    @Test
    void marksTheDatabaseUntrackedWhenAppliedVersionIsInvalid() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate(null, List.of("3.63", "legacy")));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.databaseVersion()).isEqualTo("UNTRACKED");
        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures())
                .contains("migration:expected=3.76,actual=UNTRACKED");
    }

    private SchemaReadinessService service(JdbcTemplate jdbcTemplate) {
        SchemaReadinessService service = new SchemaReadinessService(jdbcTemplate);
        ReflectionTestUtils.setField(service, "expectedVersion", "3.76");
        ReflectionTestUtils.setField(service, "requireMigrationHistory", true);
        return service;
    }

    private static final class ReadinessJdbcTemplate extends JdbcTemplate {
        private final String missingName;
        private final List<String> versions;

        private ReadinessJdbcTemplate(String missingName) {
            this(missingName, List.of("3.76"));
        }

        private ReadinessJdbcTemplate(String missingName, List<String> versions) {
            this.missingName = missingName;
            this.versions = versions;
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            boolean missing = missingName != null
                    && Arrays.stream(args).map(String::valueOf).anyMatch(missingName::equals);
            Integer count = missing ? 0 : 1;
            return requiredType.cast(count);
        }

        @Override
        public <T> List<T> queryForList(String sql, Class<T> elementType) {
            return versions.stream().map(elementType::cast).toList();
        }
    }
}
