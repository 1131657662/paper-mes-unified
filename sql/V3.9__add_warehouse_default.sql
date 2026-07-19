-- V3.9: configure the default warehouse used by outbound creation.

SET @column_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_warehouse'
    AND column_name = 'is_default'
);
SET @sql := IF(@column_missing,
  'ALTER TABLE `sys_warehouse` ADD COLUMN `is_default` TINYINT NULL COMMENT ''出库默认仓库'' AFTER `status`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE sys_warehouse SET is_default = 0 WHERE is_default IS NULL;
ALTER TABLE `sys_warehouse`
  MODIFY COLUMN `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '出库默认仓库';

SET @index_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_warehouse'
    AND index_name = 'idx_warehouse_default_status'
);
SET @sql := IF(@index_missing,
  'ALTER TABLE `sys_warehouse` ADD INDEX `idx_warehouse_default_status` (`is_default`, `status`, `is_deleted`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
