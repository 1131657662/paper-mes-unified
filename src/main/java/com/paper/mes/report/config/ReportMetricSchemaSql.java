package com.paper.mes.report.config;

import java.util.List;

final class ReportMetricSchemaSql {

    private ReportMetricSchemaSql() {
    }

    static List<String> createStatements() {
        return List.of(DEFINITION, VERSION, RELEASE, RELEASE_ITEM);
    }

    private static final String DEFINITION = """
            CREATE TABLE IF NOT EXISTS `rpt_metric_definition` (
              `uuid` varchar(36) NOT NULL,
              `metric_code` varchar(64) NOT NULL,
              `metric_name` varchar(100) NOT NULL,
              `description` varchar(500) NOT NULL DEFAULT '',
              `value_type` varchar(20) NOT NULL,
              `unit_code` varchar(20) NOT NULL,
              `display_scale` tinyint unsigned NOT NULL DEFAULT 2,
              `display_order` int unsigned NOT NULL DEFAULT 0,
              `is_enabled` tinyint NOT NULL DEFAULT 1,
              `is_deleted` tinyint NOT NULL DEFAULT 0,
              `create_by` varchar(50) DEFAULT NULL,
              `update_by` varchar(50) DEFAULT NULL,
              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              `version` int NOT NULL DEFAULT 1,
              PRIMARY KEY (`uuid`),
              UNIQUE KEY `uk_metric_definition_code` (`metric_code`),
              KEY `idx_metric_definition_enabled_order` (`is_deleted`, `is_enabled`, `display_order`, `metric_code`),
              CONSTRAINT `chk_metric_definition_value_type` CHECK (`value_type` IN ('INTEGER', 'DECIMAL', 'MONEY', 'PERCENT')),
              CONSTRAINT `chk_metric_definition_enabled` CHECK (`is_enabled` IN (0, 1)),
              CONSTRAINT `chk_metric_definition_scale` CHECK (`display_scale` <= 6)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标稳定标识'
            """;

    private static final String VERSION = """
            CREATE TABLE IF NOT EXISTS `rpt_metric_version` (
              `uuid` varchar(36) NOT NULL,
              `metric_uuid` varchar(36) NOT NULL,
              `version_no` int unsigned NOT NULL,
              `implementation_key` varchar(100) NOT NULL,
              `definition_json` json NOT NULL,
              `definition_checksum` char(64) NOT NULL,
              `version_status` tinyint NOT NULL DEFAULT 1,
              `locked_at` datetime DEFAULT NULL,
              `locked_by` varchar(36) DEFAULT NULL,
              `is_deleted` tinyint NOT NULL DEFAULT 0,
              `create_by` varchar(50) DEFAULT NULL,
              `update_by` varchar(50) DEFAULT NULL,
              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              `version` int NOT NULL DEFAULT 1,
              PRIMARY KEY (`uuid`),
              UNIQUE KEY `uk_metric_version_number` (`metric_uuid`, `version_no`),
              UNIQUE KEY `uk_metric_version_identity` (`uuid`, `metric_uuid`),
              KEY `idx_metric_version_status` (`metric_uuid`, `version_status`, `is_deleted`),
              CONSTRAINT `fk_metric_version_definition` FOREIGN KEY (`metric_uuid`)
                REFERENCES `rpt_metric_definition` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
              CONSTRAINT `chk_metric_version_number` CHECK (`version_no` >= 1),
              CONSTRAINT `chk_metric_version_status` CHECK (`version_status` IN (1, 2)),
              CONSTRAINT `chk_metric_version_lock` CHECK (
                (`version_status` = 1 AND `locked_at` IS NULL) OR
                (`version_status` = 2 AND `locked_at` IS NOT NULL)
              )
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标不可变版本'
            """;

    private static final String RELEASE = """
            CREATE TABLE IF NOT EXISTS `rpt_metric_release` (
              `uuid` varchar(36) NOT NULL,
              `release_code` varchar(40) NOT NULL,
              `release_name` varchar(120) NOT NULL,
              `release_status` tinyint NOT NULL DEFAULT 1,
              `release_checksum` char(64) DEFAULT NULL,
              `published_at` datetime DEFAULT NULL,
              `published_by` varchar(36) DEFAULT NULL,
              `retired_at` datetime DEFAULT NULL,
              `retired_by` varchar(36) DEFAULT NULL,
              `active_slot` tinyint GENERATED ALWAYS AS (
                CASE WHEN `release_status` = 2 AND `is_deleted` = 0 THEN 1 ELSE NULL END
              ) STORED,
              `is_deleted` tinyint NOT NULL DEFAULT 0,
              `create_by` varchar(50) DEFAULT NULL,
              `update_by` varchar(50) DEFAULT NULL,
              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              `version` int NOT NULL DEFAULT 1,
              PRIMARY KEY (`uuid`),
              UNIQUE KEY `uk_metric_release_code` (`release_code`),
              UNIQUE KEY `uk_metric_release_active` (`active_slot`),
              KEY `idx_metric_release_status_time` (`is_deleted`, `release_status`, `published_at`),
              CONSTRAINT `chk_metric_release_status` CHECK (`release_status` IN (1, 2, 3)),
              CONSTRAINT `chk_metric_release_lifecycle` CHECK (
                (`release_status` = 1 AND `published_at` IS NULL AND `retired_at` IS NULL) OR
                (`release_status` = 2 AND `published_at` IS NOT NULL AND `retired_at` IS NULL) OR
                (`release_status` = 3 AND `published_at` IS NOT NULL AND `retired_at` IS NOT NULL)
              )
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标原子发布包'
            """;

    private static final String RELEASE_ITEM = """
            CREATE TABLE IF NOT EXISTS `rpt_metric_release_item` (
              `uuid` varchar(36) NOT NULL,
              `release_uuid` varchar(36) NOT NULL,
              `metric_uuid` varchar(36) NOT NULL,
              `metric_version_uuid` varchar(36) NOT NULL,
              `display_order` int unsigned NOT NULL DEFAULT 0,
              `create_by` varchar(50) DEFAULT NULL,
              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (`uuid`),
              UNIQUE KEY `uk_metric_release_item_metric` (`release_uuid`, `metric_uuid`),
              KEY `idx_metric_release_item_version` (`metric_version_uuid`, `metric_uuid`),
              CONSTRAINT `fk_metric_release_item_release` FOREIGN KEY (`release_uuid`)
                REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
              CONSTRAINT `fk_metric_release_item_definition` FOREIGN KEY (`metric_uuid`)
                REFERENCES `rpt_metric_definition` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
              CONSTRAINT `fk_metric_release_item_version` FOREIGN KEY (`metric_version_uuid`, `metric_uuid`)
                REFERENCES `rpt_metric_version` (`uuid`, `metric_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布包内逐指标版本绑定'
            """;
}
