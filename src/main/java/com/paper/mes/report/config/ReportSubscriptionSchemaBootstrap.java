package com.paper.mes.report.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(35)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportSubscriptionSchemaBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ReportSubscriptionSchemaSql.createStatements().forEach(jdbcTemplate::execute);
        ensurePeriodPolicy();
        ensureReportPath();
        ensureActiveName();
        ensureRunReleaseNullable();
    }

    private void ensurePeriodPolicy() {
        addColumnIfMissing("period_policy", "tinyint NOT NULL DEFAULT 1 AFTER `report_query`");
        if (!constraintExists("chk_report_subscription_period_policy")) {
            jdbcTemplate.execute("ALTER TABLE `rpt_report_subscription` ADD CONSTRAINT "
                    + "`chk_report_subscription_period_policy` CHECK (`period_policy` IN (1, 2, 3, 4))");
        }
    }

    private void ensureReportPath() {
        addColumnIfMissing("report_path", "varchar(160) NOT NULL DEFAULT '/reports/overview' AFTER `subscription_name`");
        jdbcTemplate.update("UPDATE rpt_report_subscription SET report_path = '/reports/overview' "
                + "WHERE report_path IS NULL OR report_path = ''");
    }

    private void ensureActiveName() {
        addColumnIfMissing("active_name", "varchar(100) GENERATED ALWAYS AS "
                + "(CASE WHEN `is_deleted` = 0 THEN `subscription_name` ELSE NULL END) STORED AFTER `is_deleted`");
        if (!indexExists("uk_report_subscription_owner_active_name")) {
            jdbcTemplate.execute("ALTER TABLE `rpt_report_subscription` ADD UNIQUE KEY "
                    + "`uk_report_subscription_owner_active_name` (`owner_uuid`, `active_name`)");
        }
        if (indexExists("uk_report_subscription_owner_name")) {
            jdbcTemplate.execute("ALTER TABLE `rpt_report_subscription` DROP INDEX `uk_report_subscription_owner_name`");
        }
    }

    private void ensureRunReleaseNullable() {
        Integer required = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                        + "AND table_name = 'rpt_report_subscription_run' "
                        + "AND column_name = 'metric_release_uuid' AND is_nullable = 'NO'",
                Integer.class);
        if (required != null && required > 0) {
            jdbcTemplate.execute("ALTER TABLE `rpt_report_subscription_run` "
                    + "MODIFY COLUMN `metric_release_uuid` varchar(36) NULL");
        }
    }

    private void addColumnIfMissing(String name, String definition) {
        if (!columnExists(name)) {
            jdbcTemplate.execute("ALTER TABLE `rpt_report_subscription` ADD COLUMN `" + name + "` " + definition);
        }
    }

    private boolean columnExists(String name) {
        return count("information_schema.columns", "column_name", name) > 0;
    }

    private boolean indexExists(String name) {
        return count("information_schema.statistics", "index_name", name) > 0;
    }

    private boolean constraintExists(String name) {
        return count("information_schema.table_constraints", "constraint_name", name) > 0;
    }

    private int count(String table, String nameColumn, String name) {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE table_schema = DATABASE() "
                + "AND table_name = 'rpt_report_subscription' AND " + nameColumn + " = ?";
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, name);
        return result == null ? 0 : result;
    }
}
