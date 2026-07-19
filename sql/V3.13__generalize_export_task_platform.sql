SET SESSION lock_wait_timeout = 5;

ALTER TABLE `sys_export_task`
  ADD COLUMN `module_code` VARCHAR(30) DEFAULT NULL AFTER `task_type`,
  ADD COLUMN `operation_code` VARCHAR(50) DEFAULT NULL AFTER `module_code`,
  ADD COLUMN `request_payload` TEXT DEFAULT NULL AFTER `source_uuid`,
  ADD COLUMN `content_type` VARCHAR(120) DEFAULT NULL AFTER `file_path`,
  ADD COLUMN `attempt_count` INT NOT NULL DEFAULT 0 AFTER `expires_at`,
  ADD COLUMN `max_attempts` INT NOT NULL DEFAULT 3 AFTER `attempt_count`,
  ADD COLUMN `heartbeat_at` DATETIME DEFAULT NULL AFTER `max_attempts`,
  ADD COLUMN `worker_id` VARCHAR(100) DEFAULT NULL AFTER `heartbeat_at`,
  ADD COLUMN `downloaded_at` DATETIME DEFAULT NULL AFTER `worker_id`,
  ADD COLUMN `download_count` INT NOT NULL DEFAULT 0 AFTER `downloaded_at`,
  ADD COLUMN `cancelled_at` DATETIME DEFAULT NULL AFTER `download_count`,
  ADD COLUMN `cancelled_by` VARCHAR(36) DEFAULT NULL AFTER `cancelled_at`;

UPDATE `sys_export_task`
SET `module_code` = 'settle',
    `operation_code` = 'detail-export',
    `content_type` = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
WHERE `module_code` IS NULL;

ALTER TABLE `sys_export_task`
  MODIFY COLUMN `module_code` VARCHAR(30) NOT NULL,
  MODIFY COLUMN `operation_code` VARCHAR(50) NOT NULL,
  DROP CHECK `chk_export_task_status`,
  ADD CONSTRAINT `chk_export_task_status` CHECK (`task_status` BETWEEN 1 AND 6),
  ADD CONSTRAINT `chk_export_task_attempts`
    CHECK (`attempt_count` >= 0 AND `max_attempts` BETWEEN 1 AND 10),
  ADD CONSTRAINT `chk_export_task_download_count` CHECK (`download_count` >= 0),
  ADD KEY `idx_export_task_dispatch` (`task_status`, `heartbeat_at`, `create_time`);
