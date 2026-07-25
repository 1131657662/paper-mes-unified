package com.paper.mes.processorder.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adds settlement provenance fields to development databases without inferring historical intent. */
@Component
@RequiredArgsConstructor
@Order(44)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ProcessOrderSettlementSchemaBootstrap implements ApplicationRunner {

    private static final String TABLE = "biz_process_order";
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumn("settle_source", "VARCHAR(16) DEFAULT NULL COMMENT 'INHERIT or OVERRIDE' AFTER settle_day");
        addColumn("settle_customer_version", "INT DEFAULT NULL COMMENT 'Customer version used by settlement snapshot' AFTER settle_source");
        addColumn("settle_override_reason", "VARCHAR(200) DEFAULT NULL COMMENT 'Reason for order settlement override' AFTER settle_customer_version");
        addConstraint("chk_order_settle_source",
                "CHECK (settle_source IS NULL OR settle_source IN ('INHERIT','OVERRIDE'))");
        addConstraint("chk_order_settle_customer_version",
                "CHECK (settle_source IS NULL OR settle_customer_version IS NOT NULL)");
        addConstraint("chk_order_settle_override_reason", """
                CHECK (settle_source IS NULL
                  OR (settle_source = 'INHERIT' AND settle_override_reason IS NULL)
                  OR (settle_source = 'OVERRIDE' AND NULLIF(TRIM(settle_override_reason), '') IS NOT NULL))
                """);
    }

    private void addColumn(String column, String definition) {
        if (columnExists(column)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + TABLE + " ADD COLUMN " + column + " " + definition);
    }

    private boolean columnExists(String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, TABLE, column);
        return count != null && count > 0;
    }

    private void addConstraint(String constraint, String definition) {
        if (constraintExists(constraint)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + TABLE + " ADD CONSTRAINT " + constraint + " " + definition);
    }

    private boolean constraintExists(String constraint) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = DATABASE() AND table_name = ? AND constraint_name = ?
                """, Integer.class, TABLE, constraint);
        return count != null && count > 0;
    }
}
