-- V3.29: authenticated query snapshots for consistent report execution and export.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

CREATE TABLE IF NOT EXISTS `rpt_report_query_snapshot` (
  `uuid` VARCHAR(36) NOT NULL,
  `owner_uuid` VARCHAR(36) NOT NULL,
  `owner_role_code` VARCHAR(40) NOT NULL,
  `permission_hash` CHAR(64) NOT NULL,
  `scope_hash` CHAR(64) NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `query_hash` CHAR(64) NOT NULL,
  `idempotency_bucket` BIGINT NOT NULL,
  `query_json` JSON NOT NULL,
  `metric_version_json` JSON NOT NULL,
  `data_as_of` DATETIME(3) NOT NULL,
  `source_watermark` DATETIME(3) NOT NULL,
  `expires_at` DATETIME(3) NOT NULL,
  `snapshot_status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`uuid`),
  KEY `idx_report_query_snapshot_owner`
    (`owner_uuid`, `snapshot_status`, `expires_at`, `uuid`),
  KEY `idx_report_query_snapshot_cleanup`
    (`snapshot_status`, `expires_at`, `uuid`),
  KEY `idx_report_query_snapshot_query`
    (`owner_uuid`, `permission_hash`, `query_hash`, `metric_release_uuid`, `snapshot_status`, `create_time`),
  UNIQUE KEY `uk_report_query_snapshot_idempotency`
    (`owner_uuid`, `permission_hash`, `query_hash`, `metric_release_uuid`, `idempotency_bucket`),
  CONSTRAINT `fk_report_query_snapshot_owner` FOREIGN KEY (`owner_uuid`)
    REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_report_query_snapshot_release` FOREIGN KEY (`metric_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_report_query_snapshot_status` CHECK (`snapshot_status` IN (1, 2)),
  CONSTRAINT `chk_report_query_snapshot_expiry` CHECK (`expires_at` > `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绑定用户、权限和指标版本的报表查询快照';
