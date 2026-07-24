-- V3.26: persist subscription failures that happen before metric release resolution.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @release_required := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'rpt_report_subscription_run'
    AND column_name = 'metric_release_uuid'
    AND is_nullable = 'NO'
);
SET @sql := IF(
  @release_required > 0,
  'ALTER TABLE `rpt_report_subscription_run` MODIFY COLUMN `metric_release_uuid` VARCHAR(36) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
