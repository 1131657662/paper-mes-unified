-- V3.24: bind staged and published values to an explicit dimension set.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @stage_dimension_set_missing := (
  SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'rpt_metric_value_stage'
    AND column_name = 'dimension_set_code'
);
SET @sql := IF(@stage_dimension_set_missing,
  'ALTER TABLE `rpt_metric_value_stage` ADD COLUMN `dimension_set_code` VARCHAR(64) NOT NULL DEFAULT ''BASE'' AFTER `period_end`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @value_dimension_set_missing := (
  SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'rpt_metric_value'
    AND column_name = 'dimension_set_code'
);
SET @sql := IF(@value_dimension_set_missing,
  'ALTER TABLE `rpt_metric_value` ADD COLUMN `dimension_set_code` VARCHAR(64) NOT NULL DEFAULT ''BASE'' AFTER `period_end`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
