-- V3.49: support narrow, stable settlement candidate queries.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @candidate_index_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_order'
    AND index_name = 'idx_settle_candidate_query'
);
SET @sql := IF(@candidate_index_missing,
  'ALTER TABLE `biz_process_order` ADD INDEX `idx_settle_candidate_query` (`is_deleted`, `order_status`, `accounting_date`, `order_no`, `uuid`), ALGORITHM=INPLACE, LOCK=NONE',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
