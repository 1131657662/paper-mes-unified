-- V3.16: enforce one enabled, non-deleted default warehouse at database level.
-- Run with a short metadata-lock timeout during a maintenance window.

SET SESSION lock_wait_timeout = 5;

SET @column_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_warehouse'
    AND column_name = 'active_default_key'
);
SET @sql := IF(@column_missing,
  'ALTER TABLE `sys_warehouse` ADD COLUMN `active_default_key` TINYINT GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 AND `status` = 1 AND `is_default` = 1 THEN 1 ELSE NULL END) STORED',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_warehouse'
    AND index_name = 'uk_warehouse_active_default'
);
SET @sql := IF(@index_missing,
  'ALTER TABLE `sys_warehouse` ADD UNIQUE INDEX `uk_warehouse_active_default` (`active_default_key`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
