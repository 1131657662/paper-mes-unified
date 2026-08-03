-- V3.60: clarify that warehouse location is free-form address/identification text.
-- This migration changes metadata only; it does not modify warehouse data.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_warehouse_location_semantics', 10) INTO @warehouse_location_lock;
SET @warehouse_location_lock_sql := IF(
  @warehouse_location_lock = 1,
  'DO 0',
  'SELECT V3_60_MIGRATION_LOCK_NOT_ACQUIRED'
);
PREPARE warehouse_location_lock_guard FROM @warehouse_location_lock_sql;
EXECUTE warehouse_location_lock_guard;
DEALLOCATE PREPARE warehouse_location_lock_guard;

SET @warehouse_location_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'sys_warehouse'
     AND column_name = 'location' AND column_type = 'varchar(255)'
     AND is_nullable = 'YES' AND column_default IS NULL
     AND column_comment = '仓库地址/说明') = 0,
  'ALTER TABLE `sys_warehouse` MODIFY COLUMN `location` VARCHAR(255) DEFAULT NULL COMMENT ''仓库地址/说明''',
  'DO 0'
);
PREPARE warehouse_location_ddl FROM @warehouse_location_ddl_sql;
EXECUTE warehouse_location_ddl;
DEALLOCATE PREPARE warehouse_location_ddl;

SELECT RELEASE_LOCK('paper_mes_warehouse_location_semantics') INTO @warehouse_location_unlock;
