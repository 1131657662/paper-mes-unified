-- V3.59: allow repeated soft-delete lifecycles while keeping active business keys unique.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_active_soft_delete_uniqueness', 10) INTO @active_uniqueness_lock;
SET @active_uniqueness_lock_sql := IF(
  @active_uniqueness_lock = 1,
  'DO 0',
  'SELECT V3_59_MIGRATION_LOCK_NOT_ACQUIRED'
);
PREPARE active_uniqueness_lock_guard FROM @active_uniqueness_lock_sql;
EXECUTE active_uniqueness_lock_guard;
DEALLOCATE PREPARE active_uniqueness_lock_guard;

SET @active_uniqueness_conflicts := (
  SELECT COUNT(*)
  FROM (
    SELECT 1 FROM biz_process_config_draft
    WHERE is_deleted = 0
    GROUP BY order_uuid, original_uuid HAVING COUNT(*) > 1
    UNION ALL
    SELECT 1 FROM sys_dict_item
    WHERE is_deleted = 0
    GROUP BY dict_type, item_code HAVING COUNT(*) > 1
    UNION ALL
    SELECT 1 FROM sys_config_item
    WHERE is_deleted = 0
    GROUP BY config_key HAVING COUNT(*) > 1
    UNION ALL
    SELECT 1 FROM sys_no_rule
    WHERE is_deleted = 0
    GROUP BY biz_type HAVING COUNT(*) > 1
  ) active_duplicates
);
SET @active_uniqueness_guard_sql := IF(
  @active_uniqueness_conflicts = 0,
  'DO 0',
  'SELECT V3_59_ACTIVE_SOFT_DELETE_CONFLICTS'
);
PREPARE active_uniqueness_guard FROM @active_uniqueness_guard_sql;
EXECUTE active_uniqueness_guard;
DEALLOCATE PREPARE active_uniqueness_guard;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_process_config_draft'
     AND column_name = 'active_order_uuid') = 0,
  'ALTER TABLE `biz_process_config_draft` ADD COLUMN `active_order_uuid` VARCHAR(36) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN `order_uuid` ELSE NULL END) STORED AFTER `is_deleted`',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_process_config_draft'
     AND column_name = 'active_original_uuid') = 0,
  'ALTER TABLE `biz_process_config_draft` ADD COLUMN `active_original_uuid` VARCHAR(36) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN `original_uuid` ELSE NULL END) STORED AFTER `active_order_uuid`',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'sys_dict_item'
     AND column_name = 'active_dict_type') = 0,
  'ALTER TABLE `sys_dict_item` ADD COLUMN `active_dict_type` VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN `dict_type` ELSE NULL END) STORED AFTER `is_deleted`',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'sys_dict_item'
     AND column_name = 'active_item_code') = 0,
  'ALTER TABLE `sys_dict_item` ADD COLUMN `active_item_code` VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN `item_code` ELSE NULL END) STORED AFTER `active_dict_type`',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'sys_config_item'
     AND column_name = 'active_config_key') = 0,
  'ALTER TABLE `sys_config_item` ADD COLUMN `active_config_key` VARCHAR(80) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN `config_key` ELSE NULL END) STORED AFTER `is_deleted`',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'sys_no_rule'
     AND column_name = 'active_biz_type') = 0,
  'ALTER TABLE `sys_no_rule` ADD COLUMN `active_biz_type` VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN `biz_type` ELSE NULL END) STORED AFTER `is_deleted`',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_process_config_draft'
     AND index_name = 'uk_config_draft_roll_active') = 0,
  'ALTER TABLE `biz_process_config_draft` ADD UNIQUE KEY `uk_config_draft_roll_active` (`active_order_uuid`, `active_original_uuid`)',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'sys_dict_item'
     AND index_name = 'uk_sys_dict_item_code_active') = 0,
  'ALTER TABLE `sys_dict_item` ADD UNIQUE KEY `uk_sys_dict_item_code_active` (`active_dict_type`, `active_item_code`)',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'sys_config_item'
     AND index_name = 'uk_sys_config_key_active') = 0,
  'ALTER TABLE `sys_config_item` ADD UNIQUE KEY `uk_sys_config_key_active` (`active_config_key`)',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'sys_no_rule'
     AND index_name = 'uk_sys_no_rule_biz_active') = 0,
  'ALTER TABLE `sys_no_rule` ADD UNIQUE KEY `uk_sys_no_rule_biz_active` (`active_biz_type`)',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_process_config_draft'
     AND index_name = 'uk_config_draft_roll') > 0,
  'ALTER TABLE `biz_process_config_draft` DROP INDEX `uk_config_draft_roll`',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'sys_dict_item'
     AND index_name = 'uk_sys_dict_item_code') > 0,
  'ALTER TABLE `sys_dict_item` DROP INDEX `uk_sys_dict_item_code`',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'sys_config_item'
     AND index_name = 'uk_sys_config_key') > 0,
  'ALTER TABLE `sys_config_item` DROP INDEX `uk_sys_config_key`',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SET @active_uniqueness_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'sys_no_rule'
     AND index_name = 'uk_sys_no_rule_biz') > 0,
  'ALTER TABLE `sys_no_rule` DROP INDEX `uk_sys_no_rule_biz`',
  'DO 0'
);
PREPARE active_uniqueness_ddl FROM @active_uniqueness_ddl_sql;
EXECUTE active_uniqueness_ddl;
DEALLOCATE PREPARE active_uniqueness_ddl;

SELECT RELEASE_LOCK('paper_mes_active_soft_delete_uniqueness') INTO @active_uniqueness_unlock;
