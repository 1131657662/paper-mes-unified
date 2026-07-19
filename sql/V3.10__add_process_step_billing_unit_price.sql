-- V3.10: preserve the standard process price while allowing audited billing-price overrides.

SET @table_name := 'biz_process_step';

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'billing_unit_price') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `billing_unit_price` DECIMAL(12,4) DEFAULT NULL COMMENT ''人工核定单价，为空时沿用标准单价'' AFTER `unit_price`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE() AND table_name = @table_name
                  AND constraint_name = 'chk_process_step_billing_unit_price') = 0,
  'ALTER TABLE `biz_process_step` ADD CONSTRAINT `chk_process_step_billing_unit_price` CHECK (`billing_unit_price` IS NULL OR `billing_unit_price` > 0)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'pricing_adjustment_batch_id') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `pricing_adjustment_batch_id` VARCHAR(64) DEFAULT NULL COMMENT ''批量计价操作标识'' AFTER `pricing_adjusted_at`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
