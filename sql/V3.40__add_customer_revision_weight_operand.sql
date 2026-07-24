-- V3.40: preserve DELTA/RATIO calculation operands for customer revision audit.
-- Nullable keeps revisions created before this migration readable.

SET SESSION lock_wait_timeout = 5;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'biz_finish_customer_revision_item'
                   AND column_name = 'weight_operand') = 0,
  'ALTER TABLE `biz_finish_customer_revision_item` ADD COLUMN `weight_operand` DECIMAL(20,6) DEFAULT NULL AFTER `calculation_mode`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'biz_delivery_customer_revision_item'
                   AND column_name = 'weight_operand') = 0,
  'ALTER TABLE `biz_delivery_customer_revision_item` ADD COLUMN `weight_operand` DECIMAL(20,6) DEFAULT NULL AFTER `calculation_mode`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
