SET @process_step_machine_uuid_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_step'
    AND column_name = 'machine_uuid'
);

SET @add_process_step_machine_uuid_sql := IF(
  @process_step_machine_uuid_exists = 0,
  'ALTER TABLE biz_process_step ADD COLUMN machine_uuid VARCHAR(36) DEFAULT NULL COMMENT ''工序加工机台'' AFTER step_name',
  'SELECT 1'
);
PREPARE add_process_step_machine_uuid_stmt FROM @add_process_step_machine_uuid_sql;
EXECUTE add_process_step_machine_uuid_stmt;
DEALLOCATE PREPARE add_process_step_machine_uuid_stmt;

SET @process_step_machine_name_snap_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_step'
    AND column_name = 'machine_name_snap'
);

SET @add_process_step_machine_name_snap_sql := IF(
  @process_step_machine_name_snap_exists = 0,
  'ALTER TABLE biz_process_step ADD COLUMN machine_name_snap VARCHAR(100) DEFAULT NULL COMMENT ''工序机台名称快照'' AFTER machine_uuid',
  'SELECT 1'
);
PREPARE add_process_step_machine_name_snap_stmt FROM @add_process_step_machine_name_snap_sql;
EXECUTE add_process_step_machine_name_snap_stmt;
DEALLOCATE PREPARE add_process_step_machine_name_snap_stmt;

UPDATE biz_process_step ps
JOIN biz_original_roll r
  ON r.uuid = ps.original_uuid
LEFT JOIN sys_machine m
  ON m.uuid = r.machine_uuid
SET ps.machine_uuid = COALESCE(ps.machine_uuid, r.machine_uuid),
    ps.machine_name_snap = COALESCE(ps.machine_name_snap, m.machine_name)
WHERE ps.is_deleted = 0
  AND ps.machine_uuid IS NULL
  AND r.machine_uuid IS NOT NULL;

SET @process_step_machine_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_step'
    AND index_name = 'idx_process_step_machine_uuid'
);

SET @add_process_step_machine_index_sql := IF(
  @process_step_machine_index_exists = 0,
  'CREATE INDEX idx_process_step_machine_uuid ON biz_process_step (machine_uuid)',
  'SELECT 1'
);
PREPARE add_process_step_machine_index_stmt FROM @add_process_step_machine_index_sql;
EXECUTE add_process_step_machine_index_stmt;
DEALLOCATE PREPARE add_process_step_machine_index_stmt;
