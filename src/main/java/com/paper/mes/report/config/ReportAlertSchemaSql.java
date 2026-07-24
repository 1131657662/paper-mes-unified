package com.paper.mes.report.config;

import java.util.List;

final class ReportAlertSchemaSql {
    private ReportAlertSchemaSql() {
    }

    static List<String> createStatements() {
        return List.of(SIGNAL, RULE, EVENT, SEED_SIGNALS, SEED_RULES);
    }

    private static final String SIGNAL = """
            CREATE TABLE IF NOT EXISTS `rpt_alert_signal_definition` (
              `signal_code` varchar(64) NOT NULL,
              `signal_name` varchar(100) NOT NULL,
              `unit_code` varchar(20) NOT NULL,
              `description` varchar(500) NOT NULL DEFAULT '',
              `is_enabled` tinyint NOT NULL DEFAULT 1,
              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`signal_code`),
              CONSTRAINT `chk_alert_signal_enabled` CHECK (`is_enabled` IN (0, 1))
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表派生告警信号定义'
            """;

    private static final String RULE = """
            CREATE TABLE IF NOT EXISTS `rpt_alert_rule` (
              `uuid` varchar(36) NOT NULL,
              `signal_code` varchar(64) NOT NULL,
              `rule_name` varchar(120) NOT NULL,
              `scope_type` tinyint NOT NULL,
              `customer_uuid` varchar(36) DEFAULT NULL,
              `paper_uuid` varchar(36) DEFAULT NULL,
              `process_type` tinyint DEFAULT NULL,
              `comparison_operator` varchar(10) NOT NULL DEFAULT 'GTE',
              `threshold_value` decimal(18,6) NOT NULL,
              `severity` tinyint NOT NULL DEFAULT 1,
              `is_enabled` tinyint NOT NULL DEFAULT 1,
              `is_deleted` tinyint NOT NULL DEFAULT 0,
              `active_rule_key` varchar(180) GENERATED ALWAYS AS (
                CASE WHEN `is_deleted` = 0 THEN CONCAT(`signal_code`, ':', `scope_type`, ':',
                  COALESCE(`customer_uuid`, ''), ':', COALESCE(`paper_uuid`, ''), ':', COALESCE(`process_type`, ''))
                ELSE NULL END
              ) STORED,
              `create_by` varchar(50) DEFAULT NULL,
              `update_by` varchar(50) DEFAULT NULL,
              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              `version` int NOT NULL DEFAULT 1,
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表分层告警阈值规则'
            """;

    private static final String EVENT = """
            CREATE TABLE IF NOT EXISTS `rpt_alert_event` (
              `uuid` varchar(36) NOT NULL,
              `rule_uuid` varchar(36) NOT NULL,
              `metric_release_uuid` varchar(36) NOT NULL,
              `event_key` char(64) NOT NULL,
              `period_start` date NOT NULL,
              `period_end` date NOT NULL,
              `dimension_hash` char(64) NOT NULL,
              `metric_value` decimal(20,6) NOT NULL,
              `threshold_value` decimal(18,6) NOT NULL,
              `severity` tinyint NOT NULL,
              `event_status` tinyint NOT NULL DEFAULT 1,
              `occurrence_count` int unsigned NOT NULL DEFAULT 1,
              `first_detected_at` datetime NOT NULL,
              `last_detected_at` datetime NOT NULL,
              `resolved_at` datetime DEFAULT NULL,
              `acknowledged_at` datetime DEFAULT NULL,
              `acknowledged_by` varchar(36) DEFAULT NULL,
              `ignored_at` datetime DEFAULT NULL,
              `ignored_by` varchar(36) DEFAULT NULL,
              `ignore_reason` varchar(500) DEFAULT NULL,
              `is_deleted` tinyint NOT NULL DEFAULT 0,
              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`uuid`),
              UNIQUE KEY `uk_alert_event_key` (`event_key`),
              KEY `idx_alert_event_status_time` (`is_deleted`, `event_status`, `last_detected_at`, `uuid`),
              KEY `idx_alert_event_rule_period` (`rule_uuid`, `period_start`, `period_end`),
              KEY `idx_alert_event_release` (`metric_release_uuid`, `period_end`),
              KEY `idx_alert_event_dimension` (`dimension_hash`, `period_end`),
              KEY `idx_alert_event_acknowledged` (`event_status`, `acknowledged_at`),
              KEY `idx_alert_event_ack_by` (`acknowledged_by`),
              KEY `idx_alert_event_ignore_by` (`ignored_by`),
              CONSTRAINT `fk_alert_event_rule` FOREIGN KEY (`rule_uuid`)
                REFERENCES `rpt_alert_rule` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
              CONSTRAINT `fk_alert_event_release` FOREIGN KEY (`metric_release_uuid`)
                REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
              CONSTRAINT `fk_alert_event_ack_by` FOREIGN KEY (`acknowledged_by`)
                REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
              CONSTRAINT `fk_alert_event_ignore_by` FOREIGN KEY (`ignored_by`)
                REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
              CONSTRAINT `chk_alert_event_period` CHECK (`period_end` >= `period_start`),
              CONSTRAINT `chk_alert_event_severity` CHECK (`severity` IN (1, 2)),
              CONSTRAINT `chk_alert_event_status` CHECK (`event_status` IN (1, 2, 3)),
              CONSTRAINT `chk_alert_event_occurrences` CHECK (`occurrence_count` >= 1),
              CONSTRAINT `chk_alert_event_resolution` CHECK (
                (`event_status` = 2 AND `resolved_at` IS NOT NULL) OR (`event_status` <> 2 AND `resolved_at` IS NULL)
              )
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表告警事件审计与去重'
            """;

    private static final String SEED_SIGNALS = """
            INSERT INTO rpt_alert_signal_definition (signal_code, signal_name, unit_code, description)
            VALUES ('LOSS_RATIO', '损耗率', 'PERCENT', '原纸投入与成品产出的损耗比例'),
                   ('UNRECEIVED_RATIO', '已结算未收占比', 'PERCENT', '已结算未收金额占已结算应收比例')
            ON DUPLICATE KEY UPDATE signal_name = VALUES(signal_name), description = VALUES(description)
            """;

    private static final String SEED_RULES = """
            INSERT IGNORE INTO rpt_alert_rule
              (uuid, signal_code, rule_name, scope_type, comparison_operator, threshold_value, severity)
            VALUES ('rpt-alert-loss-global-v1', 'LOSS_RATIO', '全局损耗率预警', 1, 'GTE', 5.000000, 2),
                   ('rpt-alert-unreceived-global-v1', 'UNRECEIVED_RATIO', '全局未收占比预警', 1, 'GTE', 35.000000, 1)
            """;
}
