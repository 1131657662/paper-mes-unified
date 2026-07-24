-- V3.21: hierarchical report alert rules and deduplicated alert events.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

CREATE TABLE IF NOT EXISTS `rpt_alert_signal_definition` (
  `signal_code` VARCHAR(64) NOT NULL,
  `signal_name` VARCHAR(100) NOT NULL,
  `unit_code` VARCHAR(20) NOT NULL,
  `description` VARCHAR(500) NOT NULL DEFAULT '',
  `is_enabled` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`signal_code`),
  CONSTRAINT `chk_alert_signal_enabled` CHECK (`is_enabled` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表派生告警信号定义';

CREATE TABLE IF NOT EXISTS `rpt_alert_rule` (
  `uuid` VARCHAR(36) NOT NULL,
  `signal_code` VARCHAR(64) NOT NULL,
  `rule_name` VARCHAR(120) NOT NULL,
  `scope_type` TINYINT NOT NULL,
  `customer_uuid` VARCHAR(36) DEFAULT NULL,
  `paper_uuid` VARCHAR(36) DEFAULT NULL,
  `process_type` TINYINT DEFAULT NULL,
  `comparison_operator` VARCHAR(10) NOT NULL DEFAULT 'GTE',
  `threshold_value` DECIMAL(18,6) NOT NULL,
  `severity` TINYINT NOT NULL DEFAULT 1,
  `is_enabled` TINYINT NOT NULL DEFAULT 1,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `active_rule_key` VARCHAR(180) GENERATED ALWAYS AS (
    CASE WHEN `is_deleted` = 0 THEN CONCAT(`signal_code`, ':', `scope_type`, ':',
      COALESCE(`customer_uuid`, ''), ':', COALESCE(`paper_uuid`, ''), ':', COALESCE(`process_type`, ''))
    ELSE NULL END
  ) STORED,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_alert_rule_active_scope` (`active_rule_key`),
  KEY `idx_alert_rule_resolution` (`signal_code`, `is_deleted`, `is_enabled`, `scope_type`),
  KEY `idx_alert_rule_customer` (`customer_uuid`, `signal_code`),
  KEY `idx_alert_rule_paper` (`paper_uuid`, `signal_code`),
  CONSTRAINT `fk_alert_rule_signal` FOREIGN KEY (`signal_code`)
    REFERENCES `rpt_alert_signal_definition` (`signal_code`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_rule_customer` FOREIGN KEY (`customer_uuid`)
    REFERENCES `sys_customer` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_rule_paper` FOREIGN KEY (`paper_uuid`)
    REFERENCES `sys_paper` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_alert_rule_scope_type` CHECK (`scope_type` IN (1, 2, 3, 4)),
  CONSTRAINT `chk_alert_rule_scope_fields` CHECK (
    (`scope_type` = 1 AND `customer_uuid` IS NULL AND `paper_uuid` IS NULL AND `process_type` IS NULL) OR
    (`scope_type` = 2 AND `customer_uuid` IS NOT NULL AND `paper_uuid` IS NULL AND `process_type` IS NULL) OR
    (`scope_type` = 3 AND `customer_uuid` IS NULL AND `paper_uuid` IS NOT NULL AND `process_type` IS NULL) OR
    (`scope_type` = 4 AND `customer_uuid` IS NULL AND `paper_uuid` IS NULL AND `process_type` IN (1, 2))
  ),
  CONSTRAINT `chk_alert_rule_operator` CHECK (`comparison_operator` IN ('GT', 'GTE', 'LT', 'LTE')),
  CONSTRAINT `chk_alert_rule_threshold` CHECK (`threshold_value` >= 0),
  CONSTRAINT `chk_alert_rule_severity` CHECK (`severity` IN (1, 2)),
  CONSTRAINT `chk_alert_rule_enabled` CHECK (`is_enabled` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表分层告警阈值规则';

CREATE TABLE IF NOT EXISTS `rpt_alert_event` (
  `uuid` VARCHAR(36) NOT NULL,
  `rule_uuid` VARCHAR(36) NOT NULL,
  `metric_release_uuid` VARCHAR(36) NOT NULL,
  `event_key` CHAR(64) NOT NULL,
  `period_start` DATE NOT NULL,
  `period_end` DATE NOT NULL,
  `dimension_hash` CHAR(64) NOT NULL,
  `metric_value` DECIMAL(20,6) NOT NULL,
  `threshold_value` DECIMAL(18,6) NOT NULL,
  `severity` TINYINT NOT NULL,
  `event_status` TINYINT NOT NULL DEFAULT 1,
  `occurrence_count` INT UNSIGNED NOT NULL DEFAULT 1,
  `first_detected_at` DATETIME NOT NULL,
  `last_detected_at` DATETIME NOT NULL,
  `resolved_at` DATETIME DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_alert_event_key` (`event_key`),
  KEY `idx_alert_event_status_time` (`is_deleted`, `event_status`, `last_detected_at`, `uuid`),
  KEY `idx_alert_event_rule_period` (`rule_uuid`, `period_start`, `period_end`),
  KEY `idx_alert_event_release` (`metric_release_uuid`, `period_end`),
  KEY `idx_alert_event_dimension` (`dimension_hash`, `period_end`),
  CONSTRAINT `fk_alert_event_rule` FOREIGN KEY (`rule_uuid`)
    REFERENCES `rpt_alert_rule` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_event_release` FOREIGN KEY (`metric_release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_alert_event_period` CHECK (`period_end` >= `period_start`),
  CONSTRAINT `chk_alert_event_severity` CHECK (`severity` IN (1, 2)),
  CONSTRAINT `chk_alert_event_status` CHECK (`event_status` IN (1, 2, 3)),
  CONSTRAINT `chk_alert_event_occurrences` CHECK (`occurrence_count` >= 1),
  CONSTRAINT `chk_alert_event_resolution` CHECK (
    (`event_status` = 2 AND `resolved_at` IS NOT NULL) OR (`event_status` <> 2 AND `resolved_at` IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表告警事件审计与去重';

INSERT INTO `rpt_alert_signal_definition`
  (`signal_code`, `signal_name`, `unit_code`, `description`)
VALUES
  ('LOSS_RATIO', '损耗率', 'PERCENT', '原纸投入与成品产出的损耗比例'),
  ('UNRECEIVED_RATIO', '已结算未收占比', 'PERCENT', '已结算未收金额占已结算应收比例')
ON DUPLICATE KEY UPDATE `signal_name` = VALUES(`signal_name`), `description` = VALUES(`description`);

INSERT IGNORE INTO `rpt_alert_rule`
  (`uuid`, `signal_code`, `rule_name`, `scope_type`, `comparison_operator`, `threshold_value`, `severity`)
VALUES
  ('rpt-alert-loss-global-v1', 'LOSS_RATIO', '全局损耗率预警', 1, 'GTE', 5.000000, 2),
  ('rpt-alert-unreceived-global-v1', 'UNRECEIVED_RATIO', '全局未收占比预警', 1, 'GTE', 35.000000, 1);
