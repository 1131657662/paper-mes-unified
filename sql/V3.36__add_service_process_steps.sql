-- V3.36: model stripping/sorting and repackaging as billable process steps.
-- Safe to rerun; production deployment should keep the metadata lock timeout short.

SET SESSION lock_wait_timeout = 5;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'biz_process_step'
                   AND column_name = 'billing_basis') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `billing_basis` VARCHAR(16) DEFAULT NULL COMMENT ''服务计费基准 TON按吨 PIECE按件'' AFTER `process_weight`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'biz_settle_order'
                   AND column_name = 'service_amount') = 0,
  'ALTER TABLE `biz_settle_order` ADD COLUMN `service_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT ''整理包装等服务工序费'' AFTER `rewind_amount`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'biz_settle_detail'
                   AND column_name = 'service_amount') = 0,
  'ALTER TABLE `biz_settle_detail` ADD COLUMN `service_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT ''本单服务工序费'' AFTER `rewind_amount`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'biz_process_step'
                   AND column_name = 'service_quantity') = 0,
  'ALTER TABLE `biz_process_step` ADD COLUMN `service_quantity` DECIMAL(12,3) DEFAULT NULL COMMENT ''整理或包装服务数量'' AFTER `billing_basis`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.table_constraints
                 WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_step'
                   AND constraint_name = 'chk_process_step_service_basis') = 0,
  'ALTER TABLE `biz_process_step` ADD CONSTRAINT `chk_process_step_service_basis` CHECK (`billing_basis` IS NULL OR `billing_basis` IN (''TON'',''PIECE''))',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.table_constraints
                 WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_step'
                   AND constraint_name = 'chk_process_step_service_quantity') = 0,
  'ALTER TABLE `biz_process_step` ADD CONSTRAINT `chk_process_step_service_quantity` CHECK (`service_quantity` IS NULL OR `service_quantity` > 0)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.table_constraints
                 WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_step'
                   AND constraint_name = 'chk_process_step_type') = 0,
  'ALTER TABLE `biz_process_step` ADD CONSTRAINT `chk_process_step_type` CHECK (`step_type` IN (1,2,3,4))',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
