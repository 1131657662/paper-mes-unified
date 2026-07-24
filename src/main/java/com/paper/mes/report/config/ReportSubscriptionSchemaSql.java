package com.paper.mes.report.config;

import java.util.List;

final class ReportSubscriptionSchemaSql {

    private ReportSubscriptionSchemaSql() {
    }

    static List<String> createStatements() {
        return List.of(SUBSCRIPTION, RECIPIENT, RUN);
    }

    private static final String SUBSCRIPTION = """
            CREATE TABLE IF NOT EXISTS `rpt_report_subscription` (
              `uuid` varchar(36) NOT NULL,
              `owner_uuid` varchar(36) NOT NULL,
              `subscription_name` varchar(100) NOT NULL,
              `report_path` varchar(160) NOT NULL DEFAULT '/reports/overview',
              `schedule_type` tinyint NOT NULL,
              `execution_time` time NOT NULL,
              `week_day` tinyint DEFAULT NULL,
              `month_day` tinyint DEFAULT NULL,
              `timezone` varchar(40) NOT NULL DEFAULT 'Asia/Shanghai',
              `report_query` json NOT NULL,
              `period_policy` tinyint NOT NULL DEFAULT 1,
              `release_policy` tinyint NOT NULL DEFAULT 1,
              `pinned_release_uuid` varchar(36) DEFAULT NULL,
              `delivery_channel` varchar(20) NOT NULL DEFAULT 'DOWNLOAD_CENTER',
              `is_enabled` tinyint NOT NULL DEFAULT 1,
              `next_run_at` datetime NOT NULL,
              `last_scheduled_at` datetime DEFAULT NULL,
              `last_error_message` varchar(500) DEFAULT NULL,
              `is_deleted` tinyint NOT NULL DEFAULT 0,
              `active_name` varchar(100) GENERATED ALWAYS AS (
                CASE WHEN `is_deleted` = 0 THEN `subscription_name` ELSE NULL END
              ) STORED,
              `create_by` varchar(50) DEFAULT NULL,
              `update_by` varchar(50) DEFAULT NULL,
              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              `version` int NOT NULL DEFAULT 1,
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表定时订阅'
            """;

    private static final String RECIPIENT = """
            CREATE TABLE IF NOT EXISTS `rpt_report_subscription_recipient` (
              `uuid` varchar(36) NOT NULL,
              `subscription_uuid` varchar(36) NOT NULL,
              `recipient_uuid` varchar(36) NOT NULL,
              `create_by` varchar(50) DEFAULT NULL,
              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (`uuid`),
              UNIQUE KEY `uk_report_subscription_recipient` (`subscription_uuid`, `recipient_uuid`),
              KEY `idx_report_subscription_recipient_user` (`recipient_uuid`, `subscription_uuid`),
              CONSTRAINT `fk_report_subscription_recipient_subscription` FOREIGN KEY (`subscription_uuid`)
                REFERENCES `rpt_report_subscription` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
              CONSTRAINT `fk_report_subscription_recipient_user` FOREIGN KEY (`recipient_uuid`)
                REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表订阅接收人'
            """;

    private static final String RUN = """
            CREATE TABLE IF NOT EXISTS `rpt_report_subscription_run` (
              `uuid` varchar(36) NOT NULL,
              `subscription_uuid` varchar(36) NOT NULL,
              `scheduled_for` datetime NOT NULL,
              `metric_release_uuid` varchar(36) DEFAULT NULL,
              `run_status` tinyint NOT NULL DEFAULT 1,
              `planned_count` int unsigned NOT NULL DEFAULT 0,
              `dispatched_count` int unsigned NOT NULL DEFAULT 0,
              `failed_count` int unsigned NOT NULL DEFAULT 0,
              `error_message` varchar(500) DEFAULT NULL,
              `completed_at` datetime DEFAULT NULL,
              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表订阅调度运行记录'
            """;
}
