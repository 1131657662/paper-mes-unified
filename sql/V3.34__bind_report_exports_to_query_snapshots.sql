-- V3.34: bind report export tasks to their source page and locked metric query snapshot.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_export_task' AND column_name = 'source_path');
SET @sql := IF(@missing,
  'ALTER TABLE `sys_export_task` ADD COLUMN `source_path` VARCHAR(160) NULL AFTER `source_uuid`, ALGORITHM=INPLACE, LOCK=NONE',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_export_task' AND column_name = 'query_snapshot_uuid');
SET @sql := IF(@missing,
  'ALTER TABLE `sys_export_task` ADD COLUMN `query_snapshot_uuid` VARCHAR(36) NULL AFTER `request_payload`, ALGORITHM=INPLACE, LOCK=NONE',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_export_task' AND column_name = 'metric_release_uuid');
SET @sql := IF(@missing,
  'ALTER TABLE `sys_export_task` ADD COLUMN `metric_release_uuid` VARCHAR(36) NULL AFTER `query_snapshot_uuid`, ALGORITHM=INPLACE, LOCK=NONE',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sys_export_task' AND index_name = 'idx_export_task_query_snapshot');
SET @sql := IF(@missing,
  'ALTER TABLE `sys_export_task` ADD KEY `idx_export_task_query_snapshot` (`query_snapshot_uuid`), ALGORITHM=INPLACE, LOCK=NONE',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sys_export_task' AND index_name = 'idx_export_task_metric_release_time');
SET @sql := IF(@missing,
  'ALTER TABLE `sys_export_task` ADD KEY `idx_export_task_metric_release_time` (`metric_release_uuid`, `create_time`, `uuid`), ALGORITHM=INPLACE, LOCK=NONE',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'rpt_report_subscription' AND column_name = 'report_path');
SET @sql := IF(@missing,
  'ALTER TABLE `rpt_report_subscription` ADD COLUMN `report_path` VARCHAR(160) NOT NULL DEFAULT ''/reports/overview'' AFTER `subscription_name`, ALGORITHM=INPLACE, LOCK=NONE',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
