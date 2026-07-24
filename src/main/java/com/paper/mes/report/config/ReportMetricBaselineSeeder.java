package com.paper.mes.report.config;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReportMetricBaselineSeeder {

    private static final String LOCK_NAME = "paper_mes_report_metric_baseline";
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void seed() {
        acquireLock();
        try {
            configureChecksumSession();
            seedDefinitions();
            seedVersions();
            if (hasPublishedCurrentRelease()) return;
            seedRelease();
            seedReleaseItems();
            retirePreviousRelease();
            publishRelease();
        } finally {
            releaseLock();
        }
    }

    private void configureChecksumSession() {
        jdbcTemplate.execute("SET SESSION group_concat_max_len = 65535");
    }

    private void seedDefinitions() {
        var metrics = ReportMetricBaselineMetrics.all();
        jdbcTemplate.batchUpdate(DEFINITION_SQL, metrics, metrics.size(),
                (statement, metric) -> bindMetric(statement, metric));
    }

    private void bindMetric(PreparedStatement statement,
                            ReportMetricBaselineMetrics.MetricSeed metric) throws java.sql.SQLException {
        statement.setString(1, UUID.randomUUID().toString());
        statement.setString(2, metric.code());
        statement.setString(3, metric.name());
        statement.setString(4, metric.description());
        statement.setString(5, metric.valueType());
        statement.setString(6, metric.unitCode());
        statement.setInt(7, metric.scale());
        statement.setInt(8, metric.order());
    }

    private void seedVersions() {
        jdbcTemplate.update(VERSION_SQL);
    }

    private void seedRelease() {
        jdbcTemplate.update(RELEASE_SQL_V2, UUID.randomUUID().toString());
    }

    private void seedReleaseItems() {
        jdbcTemplate.update(RELEASE_ITEM_SQL);
    }

    private void publishRelease() {
        jdbcTemplate.update(PUBLISH_SQL);
    }

    private boolean hasPublishedCurrentRelease() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rpt_metric_release WHERE is_deleted = 0 AND release_status = 2 "
                        + "AND release_code = 'REPORT-BASELINE-V2'",
                Integer.class);
        return count != null && count > 0;
    }

    private void retirePreviousRelease() {
        jdbcTemplate.update("""
                UPDATE rpt_metric_release
                SET release_status = 3, retired_at = CURRENT_TIMESTAMP, retired_by = 'system'
                WHERE is_deleted = 0 AND release_status = 2
                  AND release_code != 'REPORT-BASELINE-V2'
                """);
    }

    private void acquireLock() {
        Integer acquired = jdbcTemplate.queryForObject("SELECT GET_LOCK(?, 10)", Integer.class, LOCK_NAME);
        if (acquired == null || acquired != 1) {
            throw new BusinessException(ResultCode.ERROR, "报表指标初始化锁获取失败");
        }
    }

    private void releaseLock() {
        jdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, LOCK_NAME);
    }

    private static final String DEFINITION_SQL = """
            INSERT IGNORE INTO rpt_metric_definition
              (uuid, metric_code, metric_name, description, value_type, unit_code, display_scale, display_order)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String VERSION_SQL = """
            INSERT IGNORE INTO rpt_metric_version
              (uuid, metric_uuid, version_no, implementation_key, definition_json,
               definition_checksum, version_status, locked_at, locked_by)
            SELECT REPLACE(UUID(), '-', ''), d.uuid, 1, CONCAT('report.sql.', d.metric_code),
                   JSON_OBJECT('implementationKey', CONCAT('report.sql.', d.metric_code), 'semanticVersion', 1),
                   SHA2(CONCAT(d.metric_code, '|1|report.sql.', d.metric_code), 256), 2, CURRENT_TIMESTAMP, 'system'
            FROM rpt_metric_definition d WHERE d.is_deleted = 0
            """;

    private static final String RELEASE_SQL = """
            INSERT IGNORE INTO rpt_metric_release
              (uuid, release_code, release_name, release_status)
            VALUES (?, 'REPORT-BASELINE-V1', '统计报表基线口径 V1', 1)
            """;

    private static final String RELEASE_SQL_V2 = """
            INSERT IGNORE INTO rpt_metric_release
              (uuid, release_code, release_name, release_status)
            VALUES (?, 'REPORT-BASELINE-V2', '统计报表全域指标口径 V2', 1)
            """;

    private static final String RELEASE_ITEM_SQL = """
            INSERT IGNORE INTO rpt_metric_release_item
              (uuid, release_uuid, metric_uuid, metric_version_uuid, display_order, create_by)
            SELECT REPLACE(UUID(), '-', ''), r.uuid, d.uuid, v.uuid, d.display_order, 'system'
            FROM rpt_metric_release r
            JOIN rpt_metric_definition d ON d.is_deleted = 0 AND d.is_enabled = 1
            JOIN rpt_metric_version v ON v.metric_uuid = d.uuid AND v.version_no = 1 AND v.is_deleted = 0
            WHERE r.release_code = 'REPORT-BASELINE-V2'
            """;

    private static final String PUBLISH_SQL = """
            UPDATE rpt_metric_release r
            SET r.release_checksum = (
                  SELECT SHA2(GROUP_CONCAT(CONCAT(d.metric_code, ':', v.definition_checksum)
                    ORDER BY d.metric_code SEPARATOR '|'), 256)
                  FROM rpt_metric_release_item i
                  JOIN rpt_metric_definition d ON d.uuid = i.metric_uuid
                  JOIN rpt_metric_version v ON v.uuid = i.metric_version_uuid
                  WHERE i.release_uuid = r.uuid
                ),
                r.release_status = 2, r.published_at = CURRENT_TIMESTAMP, r.published_by = 'system'
            WHERE r.release_code = 'REPORT-BASELINE-V2' AND r.release_status = 1
            """;
}
