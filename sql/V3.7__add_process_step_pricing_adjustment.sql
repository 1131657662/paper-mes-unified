-- V3.7: separate standard production pricing from the final billable amount.
-- Safe to rerun; production deployment should execute this before enabling the pricing UI.

SET @table_name := 'biz_process_step';

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'billing_mode') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `billing_mode` TINYINT NOT NULL DEFAULT 1 COMMENT ''1标准计价 2指定数量 3固定金额 4免收'' AFTER `step_amount`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'standard_quantity') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `standard_quantity` DECIMAL(12,3) DEFAULT NULL COMMENT ''优惠前标准计费数量'' AFTER `billing_mode`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'billing_quantity') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `billing_quantity` DECIMAL(12,3) DEFAULT NULL COMMENT ''最终计费数量'' AFTER `standard_quantity`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'billing_amount') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `billing_amount` DECIMAL(12,2) DEFAULT NULL COMMENT ''固定金额模式最终金额'' AFTER `billing_quantity`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'standard_step_amount') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `standard_step_amount` DECIMAL(12,2) DEFAULT NULL COMMENT ''优惠前标准工序金额'' AFTER `billing_amount`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'pricing_adjustment_amount') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `pricing_adjustment_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT ''最终金额减标准金额'' AFTER `standard_step_amount`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'pricing_adjustment_reason') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `pricing_adjustment_reason` VARCHAR(255) DEFAULT NULL COMMENT ''计价调整原因'' AFTER `pricing_adjustment_amount`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'pricing_adjusted_by') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `pricing_adjusted_by` VARCHAR(50) DEFAULT NULL COMMENT ''计价调整操作人'' AFTER `pricing_adjustment_reason`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = @table_name
                  AND column_name = 'pricing_adjusted_at') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `pricing_adjusted_at` DATETIME DEFAULT NULL COMMENT ''计价调整时间'' AFTER `pricing_adjusted_by`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE() AND table_name = @table_name
                  AND constraint_name = 'chk_process_step_billing_mode') = 0,
  'ALTER TABLE `biz_process_step` ADD CONSTRAINT `chk_process_step_billing_mode` CHECK (`billing_mode` IN (1,2,3,4))',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE() AND table_name = @table_name
                  AND constraint_name = 'chk_process_step_pricing_nonnegative') = 0,
  'ALTER TABLE `biz_process_step` ADD CONSTRAINT `chk_process_step_pricing_nonnegative` CHECK ((`standard_quantity` IS NULL OR `standard_quantity` >= 0) AND (`billing_quantity` IS NULL OR `billing_quantity` > 0) AND (`billing_amount` IS NULL OR `billing_amount` >= 0))',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
