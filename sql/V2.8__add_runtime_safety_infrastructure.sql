-- Runtime safety infrastructure required before production disables schema bootstraps.
SET SESSION innodb_lock_wait_timeout = 10;
SET SESSION lock_wait_timeout = 10;

CREATE TABLE IF NOT EXISTS `sys_backup_task` (
  `uuid` VARCHAR(36) NOT NULL,
  `task_type` VARCHAR(20) NOT NULL,
  `backup_id` VARCHAR(15) DEFAULT NULL,
  `task_status` VARCHAR(20) NOT NULL,
  `started_at` DATETIME NOT NULL,
  `finished_at` DATETIME DEFAULT NULL,
  `duration_ms` BIGINT DEFAULT NULL,
  `operator` VARCHAR(50) NOT NULL,
  `message` VARCHAR(255) DEFAULT NULL,
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
  PRIMARY KEY (`uuid`),
  KEY `idx_backup_task_started` (`started_at`),
  KEY `idx_backup_task_status` (`task_status`, `started_at`),
  KEY `idx_backup_task_backup` (`backup_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备份与恢复演练任务记录';

CREATE TABLE IF NOT EXISTS `sys_notification` (
  `uuid` VARCHAR(36) NOT NULL,
  `recipient_uuid` VARCHAR(36) NOT NULL,
  `notification_type` VARCHAR(30) NOT NULL,
  `severity` VARCHAR(10) NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `source_type` VARCHAR(30) NOT NULL,
  `source_uuid` VARCHAR(36) NOT NULL,
  `read_at` DATETIME DEFAULT NULL,
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
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_notification_source` (`recipient_uuid`, `notification_type`, `source_uuid`),
  KEY `idx_notification_recipient_time` (`recipient_uuid`, `create_time`),
  KEY `idx_notification_recipient_read` (`recipient_uuid`, `read_at`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统站内通知';

ALTER TABLE `sys_operation_log`
  MODIFY `remark` TEXT COMMENT '操作备注，包含回退、放行和失败原因';

SET @remaining_weight_missing := (
  SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
    AND column_name = 'remaining_weight'
);
SET @sql := IF(@remaining_weight_missing,
  'ALTER TABLE `biz_finish_roll` ADD COLUMN `remaining_weight` DECIMAL(10,3) DEFAULT NULL COMMENT ''剩余可出库重量kg，NULL按actual_weight兼容旧数据''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `biz_finish_roll`
SET `remaining_weight` = CASE
  WHEN `finish_status` = 3 THEN 0.000
  WHEN `actual_weight` IS NULL THEN 0.000
  ELSE `actual_weight`
END
WHERE `remaining_weight` IS NULL;

SET @stock_lock_missing := (
  SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_delivery_detail'
    AND column_name = 'stock_lock_status'
);
SET @sql := IF(@stock_lock_missing,
  'ALTER TABLE `biz_delivery_detail` ADD COLUMN `stock_lock_status` TINYINT NOT NULL DEFAULT 1 COMMENT ''库存占用状态：1待出库占用 0历史明细不占用''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `biz_delivery_detail` d
LEFT JOIN `biz_delivery_order` o
  ON o.uuid = d.delivery_uuid AND o.is_deleted = 0
SET d.stock_lock_status = CASE
  WHEN d.is_deleted = 0 AND o.delivery_status = 1 THEN 1
  ELSE 0
END
WHERE @stock_lock_missing = 1;

-- Never choose and delete duplicate business rows during deployment. If historical
-- pending orders reserve the same finish, the unique key below must fail so the
-- operator can stop the release and repair the conflicting orders with audit.

SET @active_finish_exists := (
  SELECT COUNT(*) > 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_delivery_detail'
    AND column_name = 'finish_uuid_active'
);
SET @active_finish_uses_lock := (
  SELECT COUNT(*) > 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_delivery_detail'
    AND column_name = 'finish_uuid_active'
    AND LOWER(generation_expression) LIKE '%stock_lock_status%'
);
SET @active_finish_index_exists := (
  SELECT COUNT(*) > 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_delivery_detail'
    AND index_name = 'uk_biz_delivery_detail_active_finish'
);
SET @rebuild_active_finish := @active_finish_exists AND NOT @active_finish_uses_lock;
SET @sql := IF(@rebuild_active_finish AND @active_finish_index_exists,
  'ALTER TABLE `biz_delivery_detail` DROP INDEX `uk_biz_delivery_detail_active_finish`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@rebuild_active_finish,
  'ALTER TABLE `biz_delivery_detail` DROP COLUMN `finish_uuid_active`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(NOT @active_finish_exists OR @rebuild_active_finish,
  'ALTER TABLE `biz_delivery_detail` ADD COLUMN `finish_uuid_active` VARCHAR(36) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 AND `stock_lock_status` = 1 THEN NULLIF(TRIM(`finish_uuid`), '''') ELSE NULL END) STORED COMMENT ''active stock lock finish roll''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @active_finish_index_exists := (
  SELECT COUNT(*) > 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_delivery_detail'
    AND index_name = 'uk_biz_delivery_detail_active_finish'
);
SET @sql := IF(NOT @active_finish_index_exists,
  'ALTER TABLE `biz_delivery_detail` ADD UNIQUE KEY `uk_biz_delivery_detail_active_finish` (`finish_uuid_active`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @original_machine_index_exists := (
  SELECT COUNT(*) > 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_original_roll'
    AND index_name = 'idx_original_roll_machine_uuid'
);
SET @sql := IF(NOT @original_machine_index_exists,
  'ALTER TABLE `biz_original_roll` ADD INDEX `idx_original_roll_machine_uuid` (`machine_uuid`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
