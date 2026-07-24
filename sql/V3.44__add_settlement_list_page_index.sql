-- V3.44: support the default settlement list filter and stable pagination order.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_settle_order'
    AND index_name = 'idx_settle_list_page'),
  'SELECT 1',
  'ALTER TABLE biz_settle_order ADD INDEX idx_settle_list_page (is_deleted, create_time, uuid)');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
