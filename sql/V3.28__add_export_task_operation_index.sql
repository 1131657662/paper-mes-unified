-- V3.28: support scoped task-center filtering by module and operation.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @missing := (
  SELECT COUNT(*) = 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sys_export_task'
    AND index_name = 'idx_export_task_owner_module_operation_time'
);
SET @sql := IF(
  @missing,
  'ALTER TABLE `sys_export_task` ADD KEY `idx_export_task_owner_module_operation_time` (`requester_uuid`, `module_code`, `operation_code`, `create_time`, `uuid`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
