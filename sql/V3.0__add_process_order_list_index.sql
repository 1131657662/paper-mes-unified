-- Allow customer-filtered process-order pages to read in create-time order
-- without sorting every matching row. Safe to rerun on MySQL 8.
SET SESSION innodb_lock_wait_timeout = 10;
SET SESSION lock_wait_timeout = 10;

SET @process_order_list_index_exists := (
  SELECT COUNT(*) > 0
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_order'
    AND index_name = 'idx_customer_deleted_ctime'
);

SET @sql := IF(
  NOT @process_order_list_index_exists,
  'ALTER TABLE `biz_process_order` ADD INDEX `idx_customer_deleted_ctime` (`customer_uuid`, `is_deleted`, `create_time`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
