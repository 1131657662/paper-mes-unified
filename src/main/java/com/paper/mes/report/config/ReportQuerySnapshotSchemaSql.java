package com.paper.mes.report.config;

import java.util.List;

final class ReportQuerySnapshotSchemaSql {
    private ReportQuerySnapshotSchemaSql() {
    }

    static List<String> createStatements() {
        return List.of(SNAPSHOT);
    }

    private static final String SNAPSHOT = """
            CREATE TABLE IF NOT EXISTS `rpt_report_query_snapshot` (
              `uuid` varchar(36) NOT NULL,
              `owner_uuid` varchar(36) NOT NULL,
              `owner_role_code` varchar(40) NOT NULL,
              `permission_hash` char(64) NOT NULL,
              `scope_hash` char(64) NOT NULL,
              `metric_release_uuid` varchar(36) NOT NULL,
              `query_hash` char(64) NOT NULL,
              `idempotency_bucket` bigint NOT NULL,
              `query_json` json NOT NULL,
              `metric_version_json` json NOT NULL,
              `data_as_of` datetime(3) NOT NULL,
              `source_watermark` datetime(3) NOT NULL,
              `expires_at` datetime(3) NOT NULL,
              `snapshot_status` tinyint NOT NULL DEFAULT 1,
              `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
              PRIMARY KEY (`uuid`),
              KEY `idx_report_query_snapshot_owner`
                (`owner_uuid`, `snapshot_status`, `expires_at`, `uuid`),
              KEY `idx_report_query_snapshot_cleanup`
                (`snapshot_status`, `expires_at`, `uuid`),
              KEY `idx_report_query_snapshot_query`
                (`owner_uuid`, `permission_hash`, `query_hash`, `metric_release_uuid`,
                 `snapshot_status`, `create_time`),
              UNIQUE KEY `uk_report_query_snapshot_idempotency`
                (`owner_uuid`, `permission_hash`, `query_hash`, `metric_release_uuid`,
                 `idempotency_bucket`),
              CONSTRAINT `fk_report_query_snapshot_owner` FOREIGN KEY (`owner_uuid`)
                REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
              CONSTRAINT `fk_report_query_snapshot_release` FOREIGN KEY (`metric_release_uuid`)
                REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
              CONSTRAINT `chk_report_query_snapshot_status` CHECK (`snapshot_status` IN (1, 2)),
              CONSTRAINT `chk_report_query_snapshot_expiry` CHECK (`expires_at` > `create_time`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绑定用户、权限和指标版本的报表查询快照'
            """;
}
