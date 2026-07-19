-- V3.8: expose standard/final pricing differences on settlement details.

SET @table_name := 'biz_settle_detail';

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'standard_process_amount') = 0,
  'ALTER TABLE `biz_settle_detail` ADD COLUMN `standard_process_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT ''优惠前标准加工费'' AFTER `rewind_amount`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'pricing_adjustment_amount') = 0,
  'ALTER TABLE `biz_settle_detail` ADD COLUMN `pricing_adjustment_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT ''最终加工费减标准加工费'' AFTER `standard_process_amount`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'pricing_adjustment_reason') = 0,
  'ALTER TABLE `biz_settle_detail` ADD COLUMN `pricing_adjustment_reason` VARCHAR(255) DEFAULT NULL COMMENT ''计价调整原因'' AFTER `pricing_adjustment_amount`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
