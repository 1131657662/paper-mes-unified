-- V3.22: idempotent materialization jobs, leased segments and atomic publication state.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

CREATE TABLE IF NOT EXISTS `rpt_metric_materialization_job` (
  `uuid` VARCHAR(36) NOT NULL,
  `task_id` VARCHAR(64) NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `period_start` DATE NOT NULL,
  `period_end` DATE NOT NULL,
  `job_status` TINYINT NOT NULL DEFAULT 1,
  `retry_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `lease_owner` VARCHAR(100) DEFAULT NULL,
  `lease_until` DATETIME DEFAULT NULL,
  `fencing_token` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `requested_by` VARCHAR(36) DEFAULT NULL,
  `started_at` DATETIME DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL,
  `error_message` VARCHAR(1000) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_materialization_task` (`task_id`),
  KEY `idx_metric_materialization_claim` (`job_status`, `lease_until`, `create_time`, `uuid`),
  KEY `idx_metric_materialization_release_period` (`metric_release_uuid`, `period_start`, `period_end`),
  CONSTRAINT `fk_metric_materialization_job_release` FOREIGN KEY (`metric_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_materialization_job_user` FOREIGN KEY (`requested_by`)
    REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_metric_materialization_job_period` CHECK (`period_end` >= `period_start`),
  CONSTRAINT `chk_metric_materialization_job_status` CHECK (`job_status` IN (1, 2, 3, 4, 5)),
  CONSTRAINT `chk_metric_materialization_job_lease` CHECK (
    (`job_status` = 2 AND `lease_owner` IS NOT NULL AND `lease_until` IS NOT NULL) OR
    (`job_status` <> 2)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标物化任务';

CREATE TABLE IF NOT EXISTS `rpt_metric_materialization_segment` (
  `uuid` VARCHAR(36) NOT NULL,
  `job_uuid` VARCHAR(36) NOT NULL,
  `segment_key` VARCHAR(100) NOT NULL,
  `period_start` DATE NOT NULL,
  `period_end` DATE NOT NULL,
  `segment_status` TINYINT NOT NULL DEFAULT 1,
  `retry_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `lease_owner` VARCHAR(100) DEFAULT NULL,
  `lease_until` DATETIME DEFAULT NULL,
  `fencing_token` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `row_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `result_checksum` CHAR(64) DEFAULT NULL,
  `started_at` DATETIME DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL,
  `error_message` VARCHAR(1000) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_materialization_segment` (`job_uuid`, `segment_key`),
  KEY `idx_metric_materialization_segment_claim` (`segment_status`, `lease_until`, `create_time`, `uuid`),
  KEY `idx_metric_materialization_segment_period` (`period_start`, `period_end`, `segment_status`),
  CONSTRAINT `fk_metric_materialization_segment_job` FOREIGN KEY (`job_uuid`)
    REFERENCES `rpt_metric_materialization_job` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_metric_materialization_segment_period` CHECK (`period_end` >= `period_start`),
  CONSTRAINT `chk_metric_materialization_segment_status` CHECK (`segment_status` IN (1, 2, 3, 4, 5)),
  CONSTRAINT `chk_metric_materialization_segment_lease` CHECK (
    (`segment_status` = 2 AND `lease_owner` IS NOT NULL AND `lease_until` IS NOT NULL) OR
    (`segment_status` <> 2)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标物化分片';

CREATE TABLE IF NOT EXISTS `rpt_metric_materialization_state` (
  `uuid` VARCHAR(36) NOT NULL,
  `task_id` VARCHAR(64) NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `metric_uuid` VARCHAR(36) NOT NULL,
  `metric_version_uuid` VARCHAR(36) NOT NULL,
  `period_start` DATE NOT NULL,
  `period_end` DATE NOT NULL,
  `dimension_set_code` VARCHAR(64) NOT NULL,
  `materialization_status` TINYINT NOT NULL DEFAULT 1,
  `retry_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `active_generation_uuid` VARCHAR(36) DEFAULT NULL,
  `source_as_of` DATETIME DEFAULT NULL,
  `materialized_at` DATETIME DEFAULT NULL,
  `result_checksum` CHAR(64) DEFAULT NULL,
  `error_message` VARCHAR(1000) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_materialization_coverage`
    (`metric_release_uuid`, `metric_version_uuid`, `period_start`, `period_end`, `dimension_set_code`),
  KEY `idx_metric_materialization_state_task` (`task_id`, `materialization_status`, `uuid`),
  KEY `idx_metric_materialization_state_period` (`period_start`, `period_end`, `materialization_status`),
  KEY `idx_metric_materialization_state_generation` (`active_generation_uuid`),
  CONSTRAINT `fk_metric_materialization_state_release` FOREIGN KEY (`metric_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_materialization_state_metric` FOREIGN KEY (`metric_uuid`)
    REFERENCES `rpt_metric_definition` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_materialization_state_version` FOREIGN KEY (`metric_version_uuid`, `metric_uuid`)
    REFERENCES `rpt_metric_version` (`uuid`, `metric_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_metric_materialization_state_period` CHECK (`period_end` >= `period_start`),
  CONSTRAINT `chk_metric_materialization_state_status` CHECK (`materialization_status` IN (1, 2, 3)),
  CONSTRAINT `chk_metric_materialization_state_publication` CHECK (
    (`materialization_status` = 2 AND `active_generation_uuid` IS NOT NULL AND `materialized_at` IS NOT NULL) OR
    (`materialization_status` <> 2)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逐指标版本物化覆盖状态';
