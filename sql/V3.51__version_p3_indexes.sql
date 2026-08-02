-- V3.51: version the former 02_index_p3-3.sql performance indexes.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'biz_process_order' AND index_name = 'idx_create_time') = 0,
  'ALTER TABLE `biz_process_order` ADD INDEX `idx_create_time` (`create_time`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'biz_delivery_order' AND index_name = 'idx_create_time') = 0,
  'ALTER TABLE `biz_delivery_order` ADD INDEX `idx_create_time` (`create_time`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'biz_settle_order' AND index_name = 'idx_create_time') = 0,
  'ALTER TABLE `biz_settle_order` ADD INDEX `idx_create_time` (`create_time`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_customer' AND index_name = 'idx_create_time') = 0,
  'ALTER TABLE `sys_customer` ADD INDEX `idx_create_time` (`create_time`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_paper' AND index_name = 'idx_create_time') = 0,
  'ALTER TABLE `sys_paper` ADD INDEX `idx_create_time` (`create_time`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_machine' AND index_name = 'idx_create_time') = 0,
  'ALTER TABLE `sys_machine` ADD INDEX `idx_create_time` (`create_time`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_warehouse' AND index_name = 'idx_create_time') = 0,
  'ALTER TABLE `sys_warehouse` ADD INDEX `idx_create_time` (`create_time`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'biz_process_step' AND index_name = 'idx_order_step_sort') = 0,
  'ALTER TABLE `biz_process_step` ADD INDEX `idx_order_step_sort` (`order_uuid`, `step_sort`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll' AND index_name = 'idx_order_row_sort') = 0,
  'ALTER TABLE `biz_finish_roll` ADD INDEX `idx_order_row_sort` (`order_uuid`, `row_sort`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'biz_process_order' AND index_name = 'idx_cust_status_ctime') = 0,
  'ALTER TABLE `biz_process_order` ADD INDEX `idx_cust_status_ctime` (`customer_uuid`, `order_status`, `create_time`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'biz_process_order' AND index_name = 'idx_customer_deleted_ctime') = 0,
  'ALTER TABLE `biz_process_order` ADD INDEX `idx_customer_deleted_ctime` (`customer_uuid`, `is_deleted`, `create_time`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
