package com.paper.mes.system.config.config;

import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.service.DocumentNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(20)
public class ArchiveCodeIntegrityBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DocumentNoService documentNoService;

    @Override
    public void run(ApplicationArguments args) {
        ensure(new ArchiveCodeTarget("sys_customer", "customer_code", NoRuleBizType.CUSTOMER,
                "uk_sys_customer_active_code", "customer_code_active"));
        ensure(new ArchiveCodeTarget("sys_paper", "paper_code", NoRuleBizType.PAPER,
                "uk_sys_paper_active_code", "paper_code_active"));
        ensure(new ArchiveCodeTarget("sys_machine", "machine_code", NoRuleBizType.MACHINE,
                "uk_sys_machine_active_code", "machine_code_active"));
        ensure(new ArchiveCodeTarget("sys_warehouse", "warehouse_code", NoRuleBizType.WAREHOUSE,
                "uk_sys_warehouse_active_code", "warehouse_code_active"));
    }

    private void ensure(ArchiveCodeTarget target) {
        backfillBlankCodes(target);
        normalizeDuplicateCodes(target);
        addActiveCodeColumn(target);
        addUniqueIndex(target);
    }

    private void backfillBlankCodes(ArchiveCodeTarget target) {
        for (String uuid : blankCodeUuids(target)) {
            updateCode(target, uuid, nextCode(target.bizType()));
        }
    }

    private void normalizeDuplicateCodes(ArchiveCodeTarget target) {
        for (DuplicateCode duplicate : duplicateCodes(target)) {
            List<String> uuids = activeUuidsByCode(target, duplicate.code());
            for (int i = 1; i < uuids.size(); i++) {
                updateCode(target, uuids.get(i), nextCode(target.bizType()));
            }
        }
    }

    private List<String> blankCodeUuids(ArchiveCodeTarget target) {
        return jdbcTemplate.queryForList("""
                SELECT uuid
                FROM %s
                WHERE is_deleted = 0
                  AND (%s IS NULL OR TRIM(%s) = '')
                ORDER BY create_time, uuid
                """.formatted(target.tableName(), target.codeColumn(), target.codeColumn()), String.class);
    }

    private List<DuplicateCode> duplicateCodes(ArchiveCodeTarget target) {
        return jdbcTemplate.query("""
                SELECT TRIM(%s) AS code
                FROM %s
                WHERE is_deleted = 0
                  AND %s IS NOT NULL
                  AND TRIM(%s) <> ''
                GROUP BY %s
                HAVING COUNT(*) > 1
                """.formatted(target.codeColumn(), target.tableName(), target.codeColumn(),
                target.codeColumn(), target.codeColumn()),
                (rs, rowNum) -> new DuplicateCode(rs.getString("code")));
    }

    private List<String> activeUuidsByCode(ArchiveCodeTarget target, String code) {
        return jdbcTemplate.queryForList("""
                SELECT uuid
                FROM %s
                WHERE is_deleted = 0
                  AND TRIM(%s) = ?
                ORDER BY create_time, uuid
                """.formatted(target.tableName(), target.codeColumn()), String.class, code);
    }

    private void updateCode(ArchiveCodeTarget target, String uuid, String code) {
        jdbcTemplate.update("""
                UPDATE %s
                SET %s = ?
                WHERE uuid = ?
                """.formatted(target.tableName(), target.codeColumn()), code, uuid);
    }

    private String nextCode(String bizType) {
        return documentNoService.next(bizType, LocalDate.now());
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
            String bizType,
            String uniqueIndexName,
            String activeCodeColumn
    ) {
    }

    private record DuplicateCode(String code) {
    }
}
