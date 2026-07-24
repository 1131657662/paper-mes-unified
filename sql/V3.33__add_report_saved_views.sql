-- V3.33: personal report saved views with optimistic locking and one default per page.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

CREATE TABLE IF NOT EXISTS `rpt_report_saved_view` (
  `uuid` VARCHAR(36) NOT NULL,
  `owner_uuid` VARCHAR(36) NOT NULL,
  `view_name` VARCHAR(100) NOT NULL,
  `report_path` VARCHAR(80) NOT NULL,
  `query_json` JSON NOT NULL,
  `dimension_code` VARCHAR(20) NULL,
  `metric_codes_json` JSON NOT NULL,
  `is_default` TINYINT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(100) NULL,
  `update_by` VARCHAR(100) NULL,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `version` INT NOT NULL DEFAULT 1,
  `active_name` VARCHAR(100) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN `view_name` ELSE NULL END
  ) STORED,
  `active_default_path` VARCHAR(80) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 AND `is_default` = 1 THEN `report_path` ELSE NULL END
  ) STORED,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_report_saved_view_owner_name` (`owner_uuid`, `active_name`),
  UNIQUE KEY `uk_report_saved_view_owner_default` (`owner_uuid`, `active_default_path`),
  KEY `idx_report_saved_view_owner` (`owner_uuid`, `is_deleted`, `update_time`, `uuid`),
  CONSTRAINT `fk_report_saved_view_owner` FOREIGN KEY (`owner_uuid`)
    REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_report_saved_view_default` CHECK (`is_default` IN (0, 1)),
  CONSTRAINT `chk_report_saved_view_deleted` CHECK (`is_deleted` IN (0, 1)),
  CONSTRAINT `chk_report_saved_view_version` CHECK (`version` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人报表保存视图';
