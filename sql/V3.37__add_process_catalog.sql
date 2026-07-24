-- V3.37: add an extensible process catalog and preserve legacy step_type values.
-- Safe to rerun. Keep metadata lock waits short during production deployment.

SET SESSION lock_wait_timeout = 5;

CREATE TABLE IF NOT EXISTS `sys_process_catalog` (
  `uuid` VARCHAR(36) NOT NULL,
  `step_type` TINYINT NOT NULL COMMENT 'Legacy-compatible process type',
  `process_code` VARCHAR(50) NOT NULL,
  `process_name` VARCHAR(80) NOT NULL,
  `process_category` VARCHAR(20) NOT NULL,
  `pricing_strategy` VARCHAR(30) NOT NULL,
  `produces_inventory_output` TINYINT NOT NULL DEFAULT 0,
  `allows_loss_recording` TINYINT NOT NULL DEFAULT 0,
  `allows_main_process` TINYINT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `sort_no` INT NOT NULL DEFAULT 100,
  `built_in` TINYINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_process_catalog_step_type` (`step_type`),
  UNIQUE KEY `uk_process_catalog_code` (`process_code`),
  KEY `idx_process_catalog_active` (`status`, `is_deleted`, `sort_no`),
  CONSTRAINT `chk_process_catalog_category` CHECK (`process_category` IN
    ('PRODUCTION','SERVICE','QUALITY','PACKAGING','LOGISTICS')),
  CONSTRAINT `chk_process_catalog_strategy` CHECK (`pricing_strategy` IN
    ('SAW_KNIFE','REWIND_WEIGHT','SERVICE_QUANTITY')),
  CONSTRAINT `chk_process_catalog_flags` CHECK (
    `produces_inventory_output` IN (0,1) AND `allows_loss_recording` IN (0,1)
    AND `allows_main_process` IN (0,1) AND `status` IN (0,1) AND `built_in` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Process capability catalog';

CREATE TABLE IF NOT EXISTS `sys_process_catalog_unit` (
  `catalog_uuid` VARCHAR(36) NOT NULL,
  `unit_code` VARCHAR(16) NOT NULL,
  `unit_name` VARCHAR(30) NOT NULL,
  `is_default` TINYINT NOT NULL DEFAULT 0,
  `default_catalog_uuid` VARCHAR(36) GENERATED ALWAYS AS
    (CASE WHEN `is_default` = 1 THEN `catalog_uuid` ELSE NULL END) STORED,
  `sort_no` INT NOT NULL DEFAULT 100,
  PRIMARY KEY (`catalog_uuid`, `unit_code`),
  UNIQUE KEY `uk_process_catalog_default_unit` (`default_catalog_uuid`),
  CONSTRAINT `fk_process_catalog_unit_catalog` FOREIGN KEY (`catalog_uuid`)
    REFERENCES `sys_process_catalog` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_process_catalog_unit_default` CHECK (`is_default` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Allowed measurement units per process';

CREATE TABLE IF NOT EXISTS `sys_process_catalog_billing_mode` (
  `catalog_uuid` VARCHAR(36) NOT NULL,
  `billing_mode` TINYINT NOT NULL COMMENT '1 standard, 2 quantity override, 3 fixed, 4 free',
  `sort_no` INT NOT NULL DEFAULT 100,
  PRIMARY KEY (`catalog_uuid`, `billing_mode`),
  CONSTRAINT `fk_process_catalog_billing_catalog` FOREIGN KEY (`catalog_uuid`)
    REFERENCES `sys_process_catalog` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_process_catalog_billing_mode` CHECK (`billing_mode` IN (1,2,3,4))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Allowed billing modes per process';

INSERT INTO `sys_process_catalog`
(`uuid`,`step_type`,`process_code`,`process_name`,`process_category`,`pricing_strategy`,
 `produces_inventory_output`,`allows_loss_recording`,`allows_main_process`,`status`,`sort_no`,`built_in`,
 `remark`,`create_by`,`update_by`) VALUES
('process-catalog-saw',1,'SAW','锯纸','PRODUCTION','SAW_KNIFE',1,1,1,1,10,1,'Legacy type 1','system','system'),
('process-catalog-rewind',2,'REWIND','复卷','PRODUCTION','REWIND_WEIGHT',1,1,1,1,20,1,'Legacy type 2','system','system'),
('process-catalog-strip',3,'STRIP_SORT','剥损整理','SERVICE','SERVICE_QUANTITY',0,1,0,1,30,1,'Legacy type 3','system','system'),
('process-catalog-repack',4,'REPACK','重新包装','PACKAGING','SERVICE_QUANTITY',0,0,0,1,40,1,'Legacy type 4','system','system')
ON DUPLICATE KEY UPDATE
  `process_name`=VALUES(`process_name`), `process_category`=VALUES(`process_category`),
  `pricing_strategy`=VALUES(`pricing_strategy`), `produces_inventory_output`=VALUES(`produces_inventory_output`),
  `allows_loss_recording`=VALUES(`allows_loss_recording`), `allows_main_process`=VALUES(`allows_main_process`),
  `sort_no`=VALUES(`sort_no`), `built_in`=1, `update_by`='system';

INSERT IGNORE INTO `sys_process_catalog_unit`
(`catalog_uuid`,`unit_code`,`unit_name`,`is_default`,`sort_no`) VALUES
('process-catalog-saw','KNIFE','刀',1,10),
('process-catalog-rewind','TON','吨',1,10),
('process-catalog-strip','PIECE','件',1,10),
('process-catalog-strip','TON','吨',0,20),
('process-catalog-repack','PIECE','件',1,10),
('process-catalog-repack','TON','吨',0,20);

INSERT IGNORE INTO `sys_process_catalog_billing_mode`
(`catalog_uuid`,`billing_mode`,`sort_no`) VALUES
('process-catalog-saw',1,10),('process-catalog-saw',2,20),
('process-catalog-saw',3,30),('process-catalog-saw',4,40),
('process-catalog-rewind',1,10),('process-catalog-rewind',2,20),
('process-catalog-rewind',3,30),('process-catalog-rewind',4,40),
('process-catalog-strip',1,10),('process-catalog-strip',3,30),('process-catalog-strip',4,40),
('process-catalog-repack',1,10),('process-catalog-repack',3,30),('process-catalog-repack',4,40);

SET @sql := IF((SELECT COUNT(*) FROM information_schema.table_constraints
                 WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_step'
                   AND constraint_name = 'chk_process_step_service_basis') > 0,
  'ALTER TABLE `biz_process_step` DROP CHECK `chk_process_step_service_basis`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.table_constraints
                 WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_step'
                   AND constraint_name = 'chk_process_step_type') > 0,
  'ALTER TABLE `biz_process_step` DROP CHECK `chk_process_step_type`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.table_constraints
                 WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_step'
                   AND constraint_name = 'fk_process_step_catalog_type') = 0,
  'ALTER TABLE `biz_process_step` ADD CONSTRAINT `fk_process_step_catalog_type` FOREIGN KEY (`step_type`) REFERENCES `sys_process_catalog` (`step_type`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
