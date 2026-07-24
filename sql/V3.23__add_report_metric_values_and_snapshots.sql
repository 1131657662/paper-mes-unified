-- V3.23: partitioned long metric values, staging and expiring report snapshots.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

CREATE TABLE IF NOT EXISTS `rpt_metric_value_stage` (
  `uuid` VARCHAR(36) NOT NULL,
  `job_uuid` VARCHAR(36) NOT NULL,
  `segment_uuid` VARCHAR(36) NOT NULL,
  `generation_uuid` VARCHAR(36) NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `metric_uuid` VARCHAR(36) NOT NULL,
  `metric_version_uuid` VARCHAR(36) NOT NULL,
  `period_start` DATE NOT NULL,
  `period_end` DATE NOT NULL,
  `dimension_set_code` VARCHAR(64) NOT NULL DEFAULT 'BASE',
  `grain_type` VARCHAR(30) NOT NULL,
  `entity_uuid` VARCHAR(36) NOT NULL DEFAULT '',
  `dimension_hash` CHAR(64) NOT NULL,
  `dimension_json` JSON NOT NULL,
  `metric_value` DECIMAL(24,6) NOT NULL,
  `source_as_of` DATETIME NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_value_stage_grain`
    (`job_uuid`, `segment_uuid`, `metric_version_uuid`, `dimension_set_code`, `grain_type`, `dimension_hash`, `entity_uuid`),
  KEY `idx_metric_value_stage_publish` (`segment_uuid`, `generation_uuid`, `period_start`, `uuid`),
  CONSTRAINT `fk_metric_value_stage_job` FOREIGN KEY (`job_uuid`)
    REFERENCES `rpt_metric_materialization_job` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_value_stage_segment` FOREIGN KEY (`segment_uuid`)
    REFERENCES `rpt_metric_materialization_segment` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_value_stage_release` FOREIGN KEY (`metric_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_value_stage_version` FOREIGN KEY (`metric_version_uuid`, `metric_uuid`)
    REFERENCES `rpt_metric_version` (`uuid`, `metric_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_metric_value_stage_period` CHECK (`period_end` >= `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标物化暂存值';

CREATE TABLE IF NOT EXISTS `rpt_metric_value` (
  `period_start` DATE NOT NULL,
  `uuid` VARCHAR(36) NOT NULL,
  `generation_uuid` VARCHAR(36) NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `metric_uuid` VARCHAR(36) NOT NULL,
  `metric_version_uuid` VARCHAR(36) NOT NULL,
  `period_end` DATE NOT NULL,
  `dimension_set_code` VARCHAR(64) NOT NULL DEFAULT 'BASE',
  `grain_type` VARCHAR(30) NOT NULL,
  `entity_uuid` VARCHAR(36) NOT NULL DEFAULT '',
  `dimension_hash` CHAR(64) NOT NULL,
  `dimension_json` JSON NOT NULL,
  `metric_value` DECIMAL(24,6) NOT NULL,
  `source_as_of` DATETIME NOT NULL,
  `published_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`period_start`, `uuid`),
  UNIQUE KEY `uk_metric_value_generation_grain`
    (`period_start`, `generation_uuid`, `metric_version_uuid`, `dimension_set_code`, `grain_type`, `dimension_hash`, `entity_uuid`),
  KEY `idx_metric_value_query`
    (`period_start`, `metric_version_uuid`, `dimension_hash`, `entity_uuid`),
  KEY `idx_metric_value_generation` (`period_start`, `generation_uuid`, `uuid`),
  CONSTRAINT `chk_metric_value_period` CHECK (`period_end` >= `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逐指标版本长表值'
PARTITION BY RANGE COLUMNS (`period_start`) (
  PARTITION `p_before_2024` VALUES LESS THAN ('2024-01-01'),
  PARTITION `p2024q1` VALUES LESS THAN ('2024-04-01'),
  PARTITION `p2024q2` VALUES LESS THAN ('2024-07-01'),
  PARTITION `p2024q3` VALUES LESS THAN ('2024-10-01'),
  PARTITION `p2024q4` VALUES LESS THAN ('2025-01-01'),
  PARTITION `p2025q1` VALUES LESS THAN ('2025-04-01'),
  PARTITION `p2025q2` VALUES LESS THAN ('2025-07-01'),
  PARTITION `p2025q3` VALUES LESS THAN ('2025-10-01'),
  PARTITION `p2025q4` VALUES LESS THAN ('2026-01-01'),
  PARTITION `p2026q1` VALUES LESS THAN ('2026-04-01'),
  PARTITION `p2026q2` VALUES LESS THAN ('2026-07-01'),
  PARTITION `p2026q3` VALUES LESS THAN ('2026-10-01'),
  PARTITION `p2026q4` VALUES LESS THAN ('2027-01-01'),
  PARTITION `p2027q1` VALUES LESS THAN ('2027-04-01'),
  PARTITION `p2027q2` VALUES LESS THAN ('2027-07-01'),
  PARTITION `p2027q3` VALUES LESS THAN ('2027-10-01'),
  PARTITION `p2027q4` VALUES LESS THAN ('2028-01-01'),
  PARTITION `p2028q1` VALUES LESS THAN ('2028-04-01'),
  PARTITION `p2028q2` VALUES LESS THAN ('2028-07-01'),
  PARTITION `p2028q3` VALUES LESS THAN ('2028-10-01'),
  PARTITION `p2028q4` VALUES LESS THAN ('2029-01-01'),
  PARTITION `p_future` VALUES LESS THAN (MAXVALUE)
);

CREATE TABLE IF NOT EXISTS `rpt_report_snapshot` (
  `uuid` VARCHAR(36) NOT NULL,
  `snapshot_key` CHAR(64) NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `report_code` VARCHAR(64) NOT NULL,
  `query_hash` CHAR(64) NOT NULL,
  `query_json` JSON NOT NULL,
  `payload_json` JSON NOT NULL,
  `source_as_of` DATETIME NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `snapshot_status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_report_snapshot_key` (`snapshot_key`),
  KEY `idx_report_snapshot_lookup` (`metric_release_uuid`, `report_code`, `query_hash`, `snapshot_status`),
  KEY `idx_report_snapshot_cleanup` (`snapshot_status`, `expires_at`, `uuid`),
  CONSTRAINT `fk_report_snapshot_release` FOREIGN KEY (`metric_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_report_snapshot_status` CHECK (`snapshot_status` IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可过期报表服务快照';

CREATE TABLE IF NOT EXISTS `rpt_report_snapshot_reference` (
  `uuid` VARCHAR(36) NOT NULL,
  `snapshot_uuid` VARCHAR(36) NOT NULL,
  `reference_type` VARCHAR(30) NOT NULL,
  `reference_uuid` VARCHAR(36) NOT NULL,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_report_snapshot_reference` (`snapshot_uuid`, `reference_type`, `reference_uuid`),
  KEY `idx_report_snapshot_reference_source` (`reference_type`, `reference_uuid`),
  CONSTRAINT `fk_report_snapshot_reference_snapshot` FOREIGN KEY (`snapshot_uuid`)
    REFERENCES `rpt_report_snapshot` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_report_snapshot_reference_type` CHECK (`reference_type` IN ('FIXED_VIEW', 'AUDIT_REPORT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表快照保留引用';
