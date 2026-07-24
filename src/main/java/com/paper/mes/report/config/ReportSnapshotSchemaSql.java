package com.paper.mes.report.config;

import java.util.List;

final class ReportSnapshotSchemaSql {
    private ReportSnapshotSchemaSql() {
    }

    static List<String> createStatements() {
        return List.of(SNAPSHOT, REFERENCE);
    }

    private static final String SNAPSHOT = """
            CREATE TABLE IF NOT EXISTS `rpt_report_snapshot` (
              `uuid` varchar(36) NOT NULL,
              `snapshot_key` char(64) NOT NULL,
              `metric_release_uuid` varchar(36) NOT NULL,
              `report_code` varchar(64) NOT NULL,
              `query_hash` char(64) NOT NULL,
              `query_json` json NOT NULL,
              `payload_json` json NOT NULL,
              `source_as_of` datetime NOT NULL,
              `expires_at` datetime NOT NULL,
              `snapshot_status` tinyint NOT NULL DEFAULT 1,
              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`uuid`),
              UNIQUE KEY `uk_report_snapshot_key` (`snapshot_key`),
              KEY `idx_report_snapshot_lookup`
                (`metric_release_uuid`, `report_code`, `query_hash`, `snapshot_status`),
              KEY `idx_report_snapshot_cleanup` (`snapshot_status`, `expires_at`, `uuid`),
              CONSTRAINT `fk_report_snapshot_release` FOREIGN KEY (`metric_release_uuid`)
                REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
              CONSTRAINT `chk_report_snapshot_status` CHECK (`snapshot_status` IN (1, 2))
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可过期报表服务快照'
            """;

    private static final String REFERENCE = """
            CREATE TABLE IF NOT EXISTS `rpt_report_snapshot_reference` (
              `uuid` varchar(36) NOT NULL,
              `snapshot_uuid` varchar(36) NOT NULL,
              `reference_type` varchar(30) NOT NULL,
              `reference_uuid` varchar(36) NOT NULL,
              `create_by` varchar(50) DEFAULT NULL,
              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (`uuid`),
              UNIQUE KEY `uk_report_snapshot_reference` (`snapshot_uuid`, `reference_type`, `reference_uuid`),
              KEY `idx_report_snapshot_reference_source` (`reference_type`, `reference_uuid`),
              CONSTRAINT `fk_report_snapshot_reference_snapshot` FOREIGN KEY (`snapshot_uuid`)
                REFERENCES `rpt_report_snapshot` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
              CONSTRAINT `chk_report_snapshot_reference_type`
                CHECK (`reference_type` IN ('FIXED_VIEW', 'AUDIT_REPORT'))
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表快照保留引用'
            """;
}
