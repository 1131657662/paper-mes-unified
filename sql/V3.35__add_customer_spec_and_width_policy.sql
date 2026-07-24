-- V3.35: preserve customer-facing specifications and explicit width-difference handling.
-- Safe to rerun. Execute with a short metadata-lock timeout before enabling the UI.

SET SESSION lock_wait_timeout = 5;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
                   AND column_name = 'customer_finish_width') = 0,
  'ALTER TABLE `biz_finish_roll` ADD COLUMN `customer_finish_width` INT DEFAULT NULL COMMENT ''客户销售门幅 mm'' AFTER `finish_width`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
                   AND column_name = 'customer_spec_override_reason') = 0,
  'ALTER TABLE `biz_finish_roll` ADD COLUMN `customer_spec_override_reason` VARCHAR(255) DEFAULT NULL COMMENT ''客户规格改写原因'' AFTER `customer_finish_width`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
                   AND column_name = 'customer_spec_override_by') = 0,
  'ALTER TABLE `biz_finish_roll` ADD COLUMN `customer_spec_override_by` VARCHAR(50) DEFAULT NULL COMMENT ''客户规格改写人'' AFTER `customer_spec_override_reason`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
                   AND column_name = 'customer_spec_override_at') = 0,
  'ALTER TABLE `biz_finish_roll` ADD COLUMN `customer_spec_override_at` DATETIME DEFAULT NULL COMMENT ''客户规格改写时间'' AFTER `customer_spec_override_by`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'biz_process_step'
                   AND column_name = 'width_difference_policy') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `width_difference_policy` VARCHAR(16) DEFAULT NULL COMMENT ''LOSS计损耗 ALLOCATE分摊 REMAINDER留余料'' AFTER `pricing_adjustment_batch_id`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'biz_process_step'
                   AND column_name = 'planned_loss_width') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `planned_loss_width` INT DEFAULT NULL COMMENT ''计划非库存损耗门幅 mm'' AFTER `width_difference_policy`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'biz_process_step'
                   AND column_name = 'planned_loss_weight') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `planned_loss_weight` DECIMAL(10,3) DEFAULT NULL COMMENT ''计划非库存损耗重量 kg'' AFTER `planned_loss_width`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.table_constraints
                 WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_step'
                   AND constraint_name = 'chk_process_step_width_policy') = 0,
  'ALTER TABLE `biz_process_step` ADD CONSTRAINT `chk_process_step_width_policy` CHECK (`width_difference_policy` IS NULL OR `width_difference_policy` IN (''LOSS'',''ALLOCATE'',''REMAINDER''))',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.table_constraints
                 WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_step'
                   AND constraint_name = 'chk_process_step_planned_loss') = 0,
  'ALTER TABLE `biz_process_step` ADD CONSTRAINT `chk_process_step_planned_loss` CHECK ((`planned_loss_width` IS NULL OR `planned_loss_width` >= 0) AND (`planned_loss_weight` IS NULL OR `planned_loss_weight` >= 0))',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
