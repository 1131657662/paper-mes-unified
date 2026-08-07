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
        assertThat(report.databaseVersion()).isEqualTo("3.63");
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

    private SchemaReadinessService service(JdbcTemplate jdbcTemplate) {
        SchemaReadinessService service = new SchemaReadinessService(jdbcTemplate);
        ReflectionTestUtils.setField(service, "expectedVersion", "3.63");
        ReflectionTestUtils.setField(service, "requireMigrationHistory", true);
        return service;
    }

    private static final class ReadinessJdbcTemplate extends JdbcTemplate {
        private final String missingName;

        private ReadinessJdbcTemplate(String missingName) {
            this.missingName = missingName;
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            String objectName = String.valueOf(args[args.length - 1]);
            Integer count = objectName.equals(missingName) ? 0 : 1;
            return requiredType.cast(count);
        }

        @Override
        public <T> List<T> queryForList(String sql, Class<T> elementType) {
            return List.of(elementType.cast("3.63"));
        }
    }
}
