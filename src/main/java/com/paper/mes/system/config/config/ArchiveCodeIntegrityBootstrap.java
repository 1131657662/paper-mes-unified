package com.paper.mes.system.config.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(20)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ArchiveCodeIntegrityBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensure(new ArchiveCodeTarget("sys_customer", "customer_code",
                "uk_sys_customer_active_code", "customer_code_active"));
        ensure(new ArchiveCodeTarget("sys_paper", "paper_code",
                "uk_sys_paper_active_code", "paper_code_active"));
        ensure(new ArchiveCodeTarget("sys_machine", "machine_code",
                "uk_sys_machine_active_code", "machine_code_active"));
        ensure(new ArchiveCodeTarget("sys_warehouse", "warehouse_code",
                "uk_sys_warehouse_active_code", "warehouse_code_active"));
    }

    private void ensure(ArchiveCodeTarget target) {
        addActiveCodeColumn(target);
        addUniqueIndex(target);
    }

    private void addActiveCodeColumn(ArchiveCodeTarget target) {
        if (columnExists(target.tableName(), target.activeCodeColumn())) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `%s`
                ADD COLUMN `%s` VARCHAR(50)
                GENERATED ALWAYS AS (
                  CASE
                    WHEN `is_deleted` = 0 THEN NULLIF(TRIM(`%s`), '')
                    ELSE NULL
                  END
                ) STORED COMMENT 'active unique code'
                """.formatted(target.tableName(), target.activeCodeColumn(), target.codeColumn()));
    }

    private void addUniqueIndex(ArchiveCodeTarget target) {
        if (indexExists(target.tableName(), target.uniqueIndexName())) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `%s`
                ADD UNIQUE KEY `%s` (`%s`)
                """.formatted(target.tableName(), target.uniqueIndexName(), target.activeCodeColumn()));
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """, Integer.class, tableName, indexName);
        return count != null && count > 0;
    }

    private record ArchiveCodeTarget(
            String tableName,
            String codeColumn,
            String uniqueIndexName,
            String activeCodeColumn
    ) {
    }
}
