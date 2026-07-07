-- V2.7: 统一加工单草稿配置表乐观锁版本默认值。
-- 目标：biz_process_config_draft.version 与其他业务表保持 DEFAULT 1。

SET SESSION lock_wait_timeout = 5;

SET @draft_table_exists := (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_config_draft'
);

SET @draft_version_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_config_draft'
    AND column_name = 'version'
);

SET @fix_draft_version_default_sql := IF(
  @draft_table_exists = 1 AND @draft_version_column_exists = 1,
  'ALTER TABLE `biz_process_config_draft` MODIFY `version` INT NOT NULL DEFAULT 1',
  'SELECT 1'
);
PREPARE fix_draft_version_default_stmt FROM @fix_draft_version_default_sql;
EXECUTE fix_draft_version_default_stmt;
DEALLOCATE PREPARE fix_draft_version_default_stmt;

SET @fix_draft_version_rows_sql := IF(
  @draft_table_exists = 1 AND @draft_version_column_exists = 1,
  'UPDATE `biz_process_config_draft` SET `version` = 1 WHERE `version` = 0',
  'SELECT 1'
);
PREPARE fix_draft_version_rows_stmt FROM @fix_draft_version_rows_sql;
EXECUTE fix_draft_version_rows_stmt;
DEALLOCATE PREPARE fix_draft_version_rows_stmt;
