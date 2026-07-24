-- V3.20: scheduled report subscriptions with per-recipient export delivery.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

CREATE TABLE IF NOT EXISTS `rpt_report_subscription` (
  `uuid` VARCHAR(36) NOT NULL,
  `owner_uuid` VARCHAR(36) NOT NULL,
  `subscription_name` VARCHAR(100) NOT NULL,
  `schedule_type` TINYINT NOT NULL,
  `execution_time` TIME NOT NULL,
  `week_day` TINYINT DEFAULT NULL,
  `month_day` TINYINT DEFAULT NULL,
  `timezone` VARCHAR(40) NOT NULL DEFAULT 'Asia/Shanghai',
  `report_query` JSON NOT NULL,
  `period_policy` TINYINT NOT NULL DEFAULT 1,
  `release_policy` TINYINT NOT NULL DEFAULT 1,
  `pinned_release_uuid` VARCHAR(36) DEFAULT NULL,
  `delivery_channel` VARCHAR(20) NOT NULL DEFAULT 'DOWNLOAD_CENTER',
  `is_enabled` TINYINT NOT NULL DEFAULT 1,
  `next_run_at` DATETIME NOT NULL,
  `last_scheduled_at` DATETIME DEFAULT NULL,
  `last_error_message` VARCHAR(500) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `active_name` VARCHAR(100) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN `subscription_name` ELSE NULL END
  ) STORED,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_report_subscription_owner_active_name` (`owner_uuid`, `active_name`),
  KEY `idx_report_subscription_due` (`is_deleted`, `is_enabled`, `next_run_at`, `uuid`),
  KEY `idx_report_subscription_release` (`pinned_release_uuid`),
  CONSTRAINT `fk_report_subscription_owner` FOREIGN KEY (`owner_uuid`)
    REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_report_subscription_release` FOREIGN KEY (`pinned_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_report_subscription_schedule_type` CHECK (`schedule_type` IN (1, 2, 3)),
  CONSTRAINT `chk_report_subscription_period_policy` CHECK (`period_policy` IN (1, 2, 3, 4)),
  CONSTRAINT `chk_report_subscription_schedule_fields` CHECK (
    (`schedule_type` = 1 AND `week_day` IS NULL AND `month_day` IS NULL) OR
    (`schedule_type` = 2 AND `week_day` BETWEEN 1 AND 7 AND `month_day` IS NULL) OR
    (`schedule_type` = 3 AND `week_day` IS NULL AND `month_day` BETWEEN 1 AND 28)
  ),
  CONSTRAINT `chk_report_subscription_release_policy` CHECK (
    (`release_policy` = 1 AND `pinned_release_uuid` IS NULL) OR
    (`release_policy` = 2 AND `pinned_release_uuid` IS NOT NULL)
  ),
  CONSTRAINT `chk_report_subscription_channel` CHECK (`delivery_channel` = 'DOWNLOAD_CENTER'),
  CONSTRAINT `chk_report_subscription_enabled` CHECK (`is_enabled` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表定时订阅';

CREATE TABLE IF NOT EXISTS `rpt_report_subscription_recipient` (
  `uuid` VARCHAR(36) NOT NULL,
  `subscription_uuid` VARCHAR(36) NOT NULL,
  `recipient_uuid` VARCHAR(36) NOT NULL,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_report_subscription_recipient` (`subscription_uuid`, `recipient_uuid`),
  KEY `idx_report_subscription_recipient_user` (`recipient_uuid`, `subscription_uuid`),
  CONSTRAINT `fk_report_subscription_recipient_subscription` FOREIGN KEY (`subscription_uuid`)
    REFERENCES `rpt_report_subscription` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_report_subscription_recipient_user` FOREIGN KEY (`recipient_uuid`)
    REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表订阅接收人';

CREATE TABLE IF NOT EXISTS `rpt_report_subscription_run` (
  `uuid` VARCHAR(36) NOT NULL,
  `subscription_uuid` VARCHAR(36) NOT NULL,
  `scheduled_for` DATETIME NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `run_status` TINYINT NOT NULL DEFAULT 1,
  `planned_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `dispatched_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `failed_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `error_message` VARCHAR(500) DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_report_subscription_run_slot` (`subscription_uuid`, `scheduled_for`),
  KEY `idx_report_subscription_run_status` (`run_status`, `scheduled_for`, `uuid`),
  KEY `idx_report_subscription_run_release` (`metric_release_uuid`, `scheduled_for`),
  CONSTRAINT `fk_report_subscription_run_subscription` FOREIGN KEY (`subscription_uuid`)
    REFERENCES `rpt_report_subscription` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_report_subscription_run_release` FOREIGN KEY (`metric_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_report_subscription_run_status` CHECK (`run_status` IN (1, 2, 3, 4)),
  CONSTRAINT `chk_report_subscription_run_counts` CHECK (
    `dispatched_count` + `failed_count` <= `planned_count`
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表订阅调度运行记录';

SET @subscription_active_name_missing := (
  SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'rpt_report_subscription'
    AND column_name = 'active_name'
);
SET @sql := IF(@subscription_active_name_missing,
  'ALTER TABLE `rpt_report_subscription` ADD COLUMN `active_name` VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN `subscription_name` ELSE NULL END) STORED AFTER `is_deleted`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @subscription_period_policy_missing := (
  SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'rpt_report_subscription'
    AND column_name = 'period_policy'
);
SET @sql := IF(@subscription_period_policy_missing,
  'ALTER TABLE `rpt_report_subscription` ADD COLUMN `period_policy` TINYINT NOT NULL DEFAULT 1 AFTER `report_query`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @subscription_period_policy_check_missing := (
  SELECT COUNT(*) = 0 FROM information_schema.table_constraints
  WHERE table_schema = DATABASE() AND table_name = 'rpt_report_subscription'
    AND constraint_name = 'chk_report_subscription_period_policy' AND constraint_type = 'CHECK'
);
SET @sql := IF(@subscription_period_policy_check_missing,
  'ALTER TABLE `rpt_report_subscription` ADD CONSTRAINT `chk_report_subscription_period_policy` CHECK (`period_policy` IN (1, 2, 3, 4))',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @subscription_active_name_index_missing := (
  SELECT COUNT(*) = 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'rpt_report_subscription'
    AND index_name = 'uk_report_subscription_owner_active_name'
);
SET @sql := IF(@subscription_active_name_index_missing,
  'ALTER TABLE `rpt_report_subscription` ADD UNIQUE KEY `uk_report_subscription_owner_active_name` (`owner_uuid`, `active_name`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_subscription_name_index_exists := (
  SELECT COUNT(*) > 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'rpt_report_subscription'
    AND index_name = 'uk_report_subscription_owner_name'
);
SET @sql := IF(@old_subscription_name_index_exists,
  'ALTER TABLE `rpt_report_subscription` DROP INDEX `uk_report_subscription_owner_name`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
