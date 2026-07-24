-- V3.18: support the default report status and accounting-date range scan.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @report_scope_index_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_order'
    AND index_name = 'idx_order_report_scope'
);
SET @sql := IF(@report_scope_index_missing,
  'ALTER TABLE `biz_process_order` ADD INDEX `idx_order_report_scope` (`is_deleted`, `order_status`, `accounting_date`, `uuid`), ALGORITHM=INPLACE, LOCK=NONE',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
