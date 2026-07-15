-- Keep active archive codes unique while allowing a soft-deleted code to be reused.
SET SESSION innodb_lock_wait_timeout = 10;
SET SESSION lock_wait_timeout = 10;

SET @customer_active_code_exists := (
  SELECT COUNT(*) > 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_customer'
    AND column_name = 'customer_code_active'
);
SET @sql := IF(NOT @customer_active_code_exists,
  'ALTER TABLE `sys_customer` ADD COLUMN `customer_code_active` VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN NULLIF(TRIM(`customer_code`), '''') ELSE NULL END) STORED COMMENT ''active unique code''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @customer_active_index_exists := (
  SELECT COUNT(*) > 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sys_customer'
    AND index_name = 'uk_sys_customer_active_code'
);
SET @sql := IF(NOT @customer_active_index_exists,
  'ALTER TABLE `sys_customer` ADD UNIQUE KEY `uk_sys_customer_active_code` (`customer_code_active`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @paper_active_code_exists := (
  SELECT COUNT(*) > 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_paper'
    AND column_name = 'paper_code_active'
);
SET @sql := IF(NOT @paper_active_code_exists,
  'ALTER TABLE `sys_paper` ADD COLUMN `paper_code_active` VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN NULLIF(TRIM(`paper_code`), '''') ELSE NULL END) STORED COMMENT ''active unique code''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @paper_active_index_exists := (
  SELECT COUNT(*) > 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sys_paper'
    AND index_name = 'uk_sys_paper_active_code'
);
SET @sql := IF(NOT @paper_active_index_exists,
  'ALTER TABLE `sys_paper` ADD UNIQUE KEY `uk_sys_paper_active_code` (`paper_code_active`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @machine_active_code_exists := (
  SELECT COUNT(*) > 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_machine'
    AND column_name = 'machine_code_active'
);
SET @sql := IF(NOT @machine_active_code_exists,
  'ALTER TABLE `sys_machine` ADD COLUMN `machine_code_active` VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN NULLIF(TRIM(`machine_code`), '''') ELSE NULL END) STORED COMMENT ''active unique code''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @machine_active_index_exists := (
  SELECT COUNT(*) > 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sys_machine'
    AND index_name = 'uk_sys_machine_active_code'
);
SET @sql := IF(NOT @machine_active_index_exists,
  'ALTER TABLE `sys_machine` ADD UNIQUE KEY `uk_sys_machine_active_code` (`machine_code_active`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @warehouse_active_code_exists := (
  SELECT COUNT(*) > 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_warehouse'
    AND column_name = 'warehouse_code_active'
);
SET @sql := IF(NOT @warehouse_active_code_exists,
  'ALTER TABLE `sys_warehouse` ADD COLUMN `warehouse_code_active` VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN NULLIF(TRIM(`warehouse_code`), '''') ELSE NULL END) STORED COMMENT ''active unique code''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @warehouse_active_index_exists := (
  SELECT COUNT(*) > 0 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sys_warehouse'
    AND index_name = 'uk_sys_warehouse_active_code'
);
SET @sql := IF(NOT @warehouse_active_index_exists,
  'ALTER TABLE `sys_warehouse` ADD UNIQUE KEY `uk_sys_warehouse_active_code` (`warehouse_code_active`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
