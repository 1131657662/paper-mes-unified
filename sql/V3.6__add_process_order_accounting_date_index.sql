-- V3.6: make settlement-period filtering indexable and safe to rerun.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @accounting_date_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_order'
    AND column_name = 'accounting_date'
);
SET @sql := IF(@accounting_date_missing,
  'ALTER TABLE `biz_process_order` ADD COLUMN `accounting_date` DATE GENERATED ALWAYS AS (COALESCE(DATE(`back_record_time`), `order_date`)) STORED COMMENT ''settlement accounting date''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @accounting_index_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_order'
    AND index_name = 'idx_order_customer_status_accounting'
);
SET @sql := IF(@accounting_index_missing,
  'ALTER TABLE `biz_process_order` ADD INDEX `idx_order_customer_status_accounting` (`customer_uuid`, `order_status`, `accounting_date`, `uuid`)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
