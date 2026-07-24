-- V3.32: composite indexes for settlement, collection, inventory and delivery report topics.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_settle_order'
    AND index_name = 'idx_report_settle_scope'),
  'SELECT 1',
  'ALTER TABLE biz_settle_order ADD INDEX idx_report_settle_scope (is_deleted, settle_status, settle_date, customer_uuid)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_receive_record'
    AND index_name = 'idx_report_receive_scope'),
  'SELECT 1',
  'ALTER TABLE biz_receive_record ADD INDEX idx_report_receive_scope (is_deleted, record_status, receive_date, settle_uuid)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
    AND index_name = 'idx_report_inventory_scope'),
  'SELECT 1',
  'ALTER TABLE biz_finish_roll ADD INDEX idx_report_inventory_scope (finish_status, is_deleted, stock_in_time, order_uuid)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_delivery_order'
    AND index_name = 'idx_report_delivery_scope'),
  'SELECT 1',
  'ALTER TABLE biz_delivery_order ADD INDEX idx_report_delivery_scope (is_deleted, delivery_status, delivery_date, customer_uuid)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_delivery_detail'
    AND index_name = 'idx_report_delivery_detail'),
  'SELECT 1',
  'ALTER TABLE biz_delivery_detail ADD INDEX idx_report_delivery_detail (is_deleted, delivery_uuid, stock_lock_status, finish_uuid)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
