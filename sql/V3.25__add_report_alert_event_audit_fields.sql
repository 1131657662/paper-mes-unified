-- V3.25: add auditable acknowledgement and ignore state to report alert events.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'rpt_alert_event' AND column_name = 'acknowledged_at');
SET @sql := IF(@missing, 'ALTER TABLE `rpt_alert_event` ADD COLUMN `acknowledged_at` DATETIME DEFAULT NULL AFTER `resolved_at`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'rpt_alert_event' AND column_name = 'acknowledged_by');
SET @sql := IF(@missing, 'ALTER TABLE `rpt_alert_event` ADD COLUMN `acknowledged_by` VARCHAR(36) DEFAULT NULL AFTER `acknowledged_at`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'rpt_alert_event' AND column_name = 'ignored_at');
SET @sql := IF(@missing, 'ALTER TABLE `rpt_alert_event` ADD COLUMN `ignored_at` DATETIME DEFAULT NULL AFTER `acknowledged_by`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'rpt_alert_event' AND column_name = 'ignored_by');
SET @sql := IF(@missing, 'ALTER TABLE `rpt_alert_event` ADD COLUMN `ignored_by` VARCHAR(36) DEFAULT NULL AFTER `ignored_at`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'rpt_alert_event' AND column_name = 'ignore_reason');
SET @sql := IF(@missing, 'ALTER TABLE `rpt_alert_event` ADD COLUMN `ignore_reason` VARCHAR(500) DEFAULT NULL AFTER `ignored_by`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'rpt_alert_event' AND index_name = 'idx_alert_event_acknowledged');
SET @sql := IF(@missing, 'ALTER TABLE `rpt_alert_event` ADD KEY `idx_alert_event_acknowledged` (`event_status`, `acknowledged_at`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'rpt_alert_event' AND index_name = 'idx_alert_event_ack_by');
SET @sql := IF(@missing, 'ALTER TABLE `rpt_alert_event` ADD KEY `idx_alert_event_ack_by` (`acknowledged_by`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'rpt_alert_event' AND index_name = 'idx_alert_event_ignore_by');
SET @sql := IF(@missing, 'ALTER TABLE `rpt_alert_event` ADD KEY `idx_alert_event_ignore_by` (`ignored_by`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'rpt_alert_event' AND constraint_name = 'fk_alert_event_ack_by');
SET @sql := IF(@missing, 'ALTER TABLE `rpt_alert_event` ADD CONSTRAINT `fk_alert_event_ack_by` FOREIGN KEY (`acknowledged_by`) REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @missing := (SELECT COUNT(*) = 0 FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'rpt_alert_event' AND constraint_name = 'fk_alert_event_ignore_by');
SET @sql := IF(@missing, 'ALTER TABLE `rpt_alert_event` ADD CONSTRAINT `fk_alert_event_ignore_by` FOREIGN KEY (`ignored_by`) REFERENCES `sys_user` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
