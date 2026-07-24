-- V3.41: model machines/workstations as production resources with per-process defaults.
-- Safe to rerun. Existing machine_type values remain as a compatibility projection.

SET SESSION lock_wait_timeout = 5;

SET @resource_kind_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_machine' AND column_name = 'resource_kind'
);
SET @sql := IF(@resource_kind_exists = 0,
  'ALTER TABLE `sys_machine` ADD COLUMN `resource_kind` VARCHAR(20) NULL COMMENT ''MACHINE设备 WORKSTATION工位'' AFTER `machine_type`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `sys_machine` SET `resource_kind` = 'MACHINE'
WHERE `resource_kind` IS NULL OR TRIM(`resource_kind`) = '';
SET @resource_kind_nullable := (
  SELECT is_nullable FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_machine' AND column_name = 'resource_kind'
);
SET @sql := IF(@resource_kind_nullable = 'YES',
  'ALTER TABLE `sys_machine` MODIFY COLUMN `resource_kind` VARCHAR(20) NOT NULL DEFAULT ''MACHINE'' COMMENT ''MACHINE设备 WORKSTATION工位''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @resource_kind_constraint_exists := (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'sys_machine'
    AND constraint_name = 'chk_machine_resource_kind'
);
SET @sql := IF(@resource_kind_constraint_exists = 0,
  'ALTER TABLE `sys_machine` ADD CONSTRAINT `chk_machine_resource_kind` CHECK (`resource_kind` IN (''MACHINE'',''WORKSTATION''))',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `sys_machine_process_capability` (
  `uuid` VARCHAR(36) NOT NULL,
  `machine_uuid` VARCHAR(36) NOT NULL,
  `catalog_uuid` VARCHAR(36) NOT NULL,
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '当前工艺默认资源',
  `priority` INT NOT NULL DEFAULT 100 COMMENT '候选排序，越小越优先',
  `min_width` INT DEFAULT NULL COMMENT '最小适用门幅mm',
  `max_width` INT DEFAULT NULL COMMENT '最大适用门幅mm',
  `max_roll_weight` DECIMAL(12,3) DEFAULT NULL COMMENT '最大卷重kg',
  `max_diameter` INT DEFAULT NULL COMMENT '最大卷径mm',
  `remark` VARCHAR(255) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  `active_machine_catalog_key` VARCHAR(80) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 THEN CONCAT(`machine_uuid`, ':', `catalog_uuid`) ELSE NULL END) STORED,
  `active_default_catalog_key` VARCHAR(36) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 AND `is_default` = 1 THEN `catalog_uuid` ELSE NULL END) STORED,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_machine_capability_active` (`active_machine_catalog_key`),
  UNIQUE KEY `uk_machine_capability_default` (`active_default_catalog_key`),
  KEY `idx_machine_capability_machine` (`machine_uuid`, `is_deleted`),
  KEY `idx_machine_capability_catalog` (`catalog_uuid`, `is_deleted`, `priority`),
  CONSTRAINT `fk_machine_capability_machine` FOREIGN KEY (`machine_uuid`)
    REFERENCES `sys_machine` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_machine_capability_catalog` FOREIGN KEY (`catalog_uuid`)
    REFERENCES `sys_process_catalog` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_machine_capability_default` CHECK (`is_default` IN (0,1)),
  CONSTRAINT `chk_machine_capability_priority` CHECK (`priority` BETWEEN 1 AND 9999),
  CONSTRAINT `chk_machine_capability_width` CHECK (
    (`min_width` IS NULL OR `min_width` > 0) AND (`max_width` IS NULL OR `max_width` > 0)
    AND (`min_width` IS NULL OR `max_width` IS NULL OR `min_width` <= `max_width`)),
  CONSTRAINT `chk_machine_capability_limits` CHECK (
    (`max_roll_weight` IS NULL OR `max_roll_weight` > 0)
    AND (`max_diameter` IS NULL OR `max_diameter` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机台/工位支持的工艺能力';

INSERT IGNORE INTO `sys_machine_process_capability`
(`uuid`,`machine_uuid`,`catalog_uuid`,`priority`,`remark`,`create_by`,`update_by`)
SELECT UUID(), m.uuid, c.uuid, 100, '由历史机台类型迁移', 'system', 'system'
FROM `sys_machine` m
JOIN `sys_process_catalog` c
  ON (m.machine_type = 1 AND c.process_code = 'SAW')
  OR (m.machine_type = 2 AND c.process_code = 'REWIND')
  OR (m.machine_type = 3 AND c.process_code IN ('SAW','REWIND'))
WHERE m.is_deleted = 0 AND c.is_deleted = 0;

UPDATE `sys_machine_process_capability` capability
JOIN (
  SELECT MIN(mc.uuid) AS capability_uuid
  FROM `sys_machine_process_capability` mc
  JOIN `sys_machine` m ON m.uuid = mc.machine_uuid
  WHERE mc.is_deleted = 0 AND m.is_deleted = 0 AND m.status = 1
  GROUP BY mc.catalog_uuid
  HAVING COUNT(*) = 1
) single_capability ON single_capability.capability_uuid = capability.uuid
SET capability.is_default = 1
WHERE capability.is_default = 0;
