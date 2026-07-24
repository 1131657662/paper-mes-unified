-- V3.19: versioned report metric semantics and atomic release bundles.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;
SET SESSION group_concat_max_len = 65535;

CREATE TABLE IF NOT EXISTS `rpt_metric_definition` (
  `uuid` VARCHAR(36) NOT NULL,
  `metric_code` VARCHAR(64) NOT NULL,
  `metric_name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500) NOT NULL DEFAULT '',
  `value_type` VARCHAR(20) NOT NULL,
  `unit_code` VARCHAR(20) NOT NULL,
  `display_scale` TINYINT UNSIGNED NOT NULL DEFAULT 2,
  `display_order` INT UNSIGNED NOT NULL DEFAULT 0,
  `is_enabled` TINYINT NOT NULL DEFAULT 1,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_definition_code` (`metric_code`),
  KEY `idx_metric_definition_enabled_order` (`is_deleted`, `is_enabled`, `display_order`, `metric_code`),
  CONSTRAINT `chk_metric_definition_value_type` CHECK (`value_type` IN ('INTEGER', 'DECIMAL', 'MONEY', 'PERCENT')),
  CONSTRAINT `chk_metric_definition_enabled` CHECK (`is_enabled` IN (0, 1)),
  CONSTRAINT `chk_metric_definition_scale` CHECK (`display_scale` <= 6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标稳定标识';

CREATE TABLE IF NOT EXISTS `rpt_metric_version` (
  `uuid` VARCHAR(36) NOT NULL,
  `metric_uuid` VARCHAR(36) NOT NULL,
  `version_no` INT UNSIGNED NOT NULL,
  `implementation_key` VARCHAR(100) NOT NULL,
  `definition_json` JSON NOT NULL,
  `definition_checksum` CHAR(64) NOT NULL,
  `version_status` TINYINT NOT NULL DEFAULT 1,
  `locked_at` DATETIME DEFAULT NULL,
  `locked_by` VARCHAR(36) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标不可变版本';

CREATE TABLE IF NOT EXISTS `rpt_metric_release` (
  `uuid` VARCHAR(36) NOT NULL,
  `release_code` VARCHAR(40) NOT NULL,
  `release_name` VARCHAR(120) NOT NULL,
  `release_status` TINYINT NOT NULL DEFAULT 1,
  `release_checksum` CHAR(64) DEFAULT NULL,
  `published_at` DATETIME DEFAULT NULL,
  `published_by` VARCHAR(36) DEFAULT NULL,
  `retired_at` DATETIME DEFAULT NULL,
  `retired_by` VARCHAR(36) DEFAULT NULL,
  `active_slot` TINYINT GENERATED ALWAYS AS (
    CASE WHEN `release_status` = 2 AND `is_deleted` = 0 THEN 1 ELSE NULL END
  ) STORED,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标原子发布包';

CREATE TABLE IF NOT EXISTS `rpt_metric_release_item` (
  `uuid` VARCHAR(36) NOT NULL,
  `release_uuid` VARCHAR(36) NOT NULL,
  `metric_uuid` VARCHAR(36) NOT NULL,
  `metric_version_uuid` VARCHAR(36) NOT NULL,
  `display_order` INT UNSIGNED NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_metric_release_item_metric` (`release_uuid`, `metric_uuid`),
  KEY `idx_metric_release_item_version` (`metric_version_uuid`, `metric_uuid`),
  CONSTRAINT `fk_metric_release_item_release` FOREIGN KEY (`release_uuid`)
    REFERENCES `rpt_metric_release` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_release_item_definition` FOREIGN KEY (`metric_uuid`)
    REFERENCES `rpt_metric_definition` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_metric_release_item_version` FOREIGN KEY (`metric_version_uuid`, `metric_uuid`)
    REFERENCES `rpt_metric_version` (`uuid`, `metric_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布包内逐指标版本绑定';

INSERT INTO `rpt_metric_definition`
  (`uuid`, `metric_code`, `metric_name`, `description`, `value_type`, `unit_code`, `display_scale`, `display_order`)
VALUES
  (REPLACE(UUID(), '-', ''), 'order_count', '加工单数', '符合筛选条件的有效加工单数量', 'INTEGER', 'ORDER', 0, 10),
  (REPLACE(UUID(), '-', ''), 'original_roll_count', '原卷数', '非直发有效原卷数量', 'INTEGER', 'ROLL', 0, 20),
  (REPLACE(UUID(), '-', ''), 'finish_roll_count', '成品数', '有效最终成品数量', 'INTEGER', 'ROLL', 0, 30),
  (REPLACE(UUID(), '-', ''), 'original_weight_kg', '原纸重量', '有效原卷实际优先重量，单位千克', 'DECIMAL', 'KG', 3, 40),
  (REPLACE(UUID(), '-', ''), 'finish_weight_kg', '成品重量', '有效最终成品实际优先重量，单位千克', 'DECIMAL', 'KG', 3, 50),
  (REPLACE(UUID(), '-', ''), 'loss_weight_kg', '损耗重量', '原卷累计损耗重量，单位千克', 'DECIMAL', 'KG', 3, 60),
  (REPLACE(UUID(), '-', ''), 'loss_ratio_pct', '损耗率', '损耗重量除以原纸重量乘以100', 'PERCENT', 'PERCENT', 2, 70),
  (REPLACE(UUID(), '-', ''), 'knife_count', '刀数', '有效加工工序刀数合计', 'INTEGER', 'KNIFE', 0, 80),
  (REPLACE(UUID(), '-', ''), 'saw_amount', '锯纸费', '锯纸工序费用合计', 'MONEY', 'CNY', 2, 90),
  (REPLACE(UUID(), '-', ''), 'rewind_amount', '复卷费', '复卷工序费用合计', 'MONEY', 'CNY', 2, 100),
  (REPLACE(UUID(), '-', ''), 'process_amount', '加工费', '加工费用合计', 'MONEY', 'CNY', 2, 110),
  (REPLACE(UUID(), '-', ''), 'extra_amount', '附加费', '加工单附加费用合计', 'MONEY', 'CNY', 2, 120),
  (REPLACE(UUID(), '-', ''), 'total_amount', '应收合计', '有效应收金额合计', 'MONEY', 'CNY', 2, 130),
  (REPLACE(UUID(), '-', ''), 'settled_amount', '已结算应收', '有效结算单覆盖的应收金额', 'MONEY', 'CNY', 2, 140),
  (REPLACE(UUID(), '-', ''), 'pending_settle_amount', '待结算应收', '尚未进入有效结算单的应收金额', 'MONEY', 'CNY', 2, 150),
  (REPLACE(UUID(), '-', ''), 'received_amount', '已收金额', '有效收款流水总额', 'MONEY', 'CNY', 2, 160),
  (REPLACE(UUID(), '-', ''), 'cash_received_amount', '现金到账', '有效收款流水中的现金金额', 'MONEY', 'CNY', 2, 170),
  (REPLACE(UUID(), '-', ''), 'scrap_offset_amount', '废纸抵扣', '有效收款流水中的废纸抵扣金额', 'MONEY', 'CNY', 2, 180),
  (REPLACE(UUID(), '-', ''), 'unreceived_amount', '已结算未收', '已结算应收减有效已收金额，不小于零', 'MONEY', 'CNY', 2, 190)
ON DUPLICATE KEY UPDATE `uuid` = `uuid`;

INSERT INTO `rpt_metric_version`
  (`uuid`, `metric_uuid`, `version_no`, `implementation_key`, `definition_json`,
   `definition_checksum`, `version_status`, `locked_at`, `locked_by`)
SELECT REPLACE(UUID(), '-', ''), d.uuid, 1, CONCAT('report.sql.', d.metric_code),
       JSON_OBJECT('implementationKey', CONCAT('report.sql.', d.metric_code), 'semanticVersion', 1),
       SHA2(CONCAT(d.metric_code, '|1|report.sql.', d.metric_code), 256), 2, CURRENT_TIMESTAMP, 'system'
FROM `rpt_metric_definition` d
WHERE d.is_deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM `rpt_metric_version` v
    WHERE v.metric_uuid = d.uuid AND v.version_no = 1
  );

INSERT INTO `rpt_metric_release`
  (`uuid`, `release_code`, `release_name`, `release_status`)
SELECT REPLACE(UUID(), '-', ''), 'REPORT-BASELINE-V1', '统计报表基线口径 V1', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `rpt_metric_release` WHERE release_code = 'REPORT-BASELINE-V1'
);

INSERT INTO `rpt_metric_release_item`
  (`uuid`, `release_uuid`, `metric_uuid`, `metric_version_uuid`, `display_order`, `create_by`)
SELECT REPLACE(UUID(), '-', ''), r.uuid, d.uuid, v.uuid, d.display_order, 'system'
FROM `rpt_metric_release` r
JOIN `rpt_metric_definition` d ON d.is_deleted = 0 AND d.is_enabled = 1
JOIN `rpt_metric_version` v ON v.metric_uuid = d.uuid AND v.version_no = 1 AND v.is_deleted = 0
WHERE r.release_code = 'REPORT-BASELINE-V1'
  AND NOT EXISTS (
    SELECT 1 FROM `rpt_metric_release_item` i
    WHERE i.release_uuid = r.uuid AND i.metric_uuid = d.uuid
  );

UPDATE `rpt_metric_release` r
SET r.release_checksum = (
      SELECT SHA2(GROUP_CONCAT(CONCAT(d.metric_code, ':', v.definition_checksum)
        ORDER BY d.metric_code SEPARATOR '|'), 256)
      FROM `rpt_metric_release_item` i
      JOIN `rpt_metric_definition` d ON d.uuid = i.metric_uuid
      JOIN `rpt_metric_version` v ON v.uuid = i.metric_version_uuid
      WHERE i.release_uuid = r.uuid
    ),
    r.release_status = 2,
    r.published_at = COALESCE(r.published_at, CURRENT_TIMESTAMP),
    r.published_by = COALESCE(r.published_by, 'system')
WHERE r.release_code = 'REPORT-BASELINE-V1'
  AND r.release_status IN (1, 2);
