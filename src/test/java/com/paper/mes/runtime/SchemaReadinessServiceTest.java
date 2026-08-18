package com.paper.mes.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaReadinessServiceTest {

    @Test
    void reportsReadyWhenAllCriticalStructuresExist() {
        SchemaReadinessService service = service(new ReadinessJdbcTemplate(null));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isTrue();
        assertThat(report.databaseVersion()).isEqualTo("3.73");
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
    void reportsMissingProjectMemoryEvidenceParseConstraint() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate("fk_memory_evidence_parse"));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures()).containsExactly(
                "constraint:biz_project_memory_candidate_evidence.fk_memory_evidence_parse");
    }

    @Test
    void selectsTheHighestSemanticMigrationVersion() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate(null, List.of("3.61", "3.9", "3.63")));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.databaseVersion()).isEqualTo("3.63");
        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures())
                .contains("migration:expected=3.73,actual=3.63");
    }

    @Test
    void marksTheDatabaseUntrackedWhenAppliedVersionIsInvalid() {
        SchemaReadinessService service = service(
                new ReadinessJdbcTemplate(null, List.of("3.63", "legacy")));

        SchemaReadinessReport report = service.refresh();

        assertThat(report.databaseVersion()).isEqualTo("UNTRACKED");
        assertThat(report.ready()).isFalse();
        assertThat(report.missingStructures())
                .contains("migration:expected=3.73,actual=UNTRACKED");
    }

    private SchemaReadinessService service(JdbcTemplate jdbcTemplate) {
        SchemaReadinessService service = new SchemaReadinessService(jdbcTemplate);
        ReflectionTestUtils.setField(service, "expectedVersion", "3.73");
        ReflectionTestUtils.setField(service, "requireMigrationHistory", true);
        return service;
    }

    private static final class ReadinessJdbcTemplate extends JdbcTemplate {
        private final String missingName;
        private final List<String> versions;

        private ReadinessJdbcTemplate(String missingName) {
            this(missingName, List.of("3.73"));
        }

        private ReadinessJdbcTemplate(String missingName, List<String> versions) {
            this.missingName = missingName;
            this.versions = versions;
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            String objectName = String.valueOf(args[args.length - 1]);
            Integer count = objectName.equals(missingName) ? 0 : 1;
            return requiredType.cast(count);
        }

        @Override
        public <T> List<T> queryForList(String sql, Class<T> elementType) {
            return versions.stream().map(elementType::cast).toList();
        }
    }
}
