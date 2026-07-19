SET SESSION lock_wait_timeout = 5;

SET @export_task_operations_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_export_task'
    AND index_name = 'idx_export_task_status_completed'
);

SET @export_task_operations_index_sql = IF(
  @export_task_operations_index_exists = 0,
  'ALTER TABLE `sys_export_task` ADD KEY `idx_export_task_status_completed` (`task_status`, `completed_at`), ALGORITHM=INPLACE, LOCK=NONE',
  'SELECT 1'
);

PREPARE export_task_operations_index_statement FROM @export_task_operations_index_sql;
EXECUTE export_task_operations_index_statement;
DEALLOCATE PREPARE export_task_operations_index_statement;
