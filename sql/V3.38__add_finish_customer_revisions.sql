-- V3.38: version customer-facing finish specifications without changing physical facts.
-- Safe to rerun. Keep metadata lock waits short during production deployment.

SET SESSION lock_wait_timeout = 5;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
                   AND column_name = 'customer_display_weight') = 0,
  'ALTER TABLE `biz_finish_roll` ADD COLUMN `customer_display_weight` DECIMAL(12,3) DEFAULT NULL COMMENT ''Customer-facing weight kg'' AFTER `customer_finish_width`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `biz_finish_customer_revision` (
  `uuid` VARCHAR(36) NOT NULL,
  `order_uuid` VARCHAR(36) NOT NULL,
  `revision_no` INT NOT NULL,
  `request_id` VARCHAR(64) NOT NULL,
  `request_hash` CHAR(64) NOT NULL,
  `source_stage` VARCHAR(20) NOT NULL,
  `reason` VARCHAR(255) NOT NULL,
  `item_count` INT NOT NULL,
  `customer_total_weight` DECIMAL(14,3) DEFAULT NULL,
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
  UNIQUE KEY `uk_finish_customer_revision_no` (`order_uuid`, `revision_no`),
  UNIQUE KEY `uk_finish_customer_revision_request` (`order_uuid`, `request_id`),
  KEY `idx_finish_customer_revision_history` (`order_uuid`, `is_deleted`, `revision_no`),
  CONSTRAINT `fk_finish_customer_revision_order` FOREIGN KEY (`order_uuid`)
    REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_finish_customer_revision_no` CHECK (`revision_no` >= 1),
  CONSTRAINT `chk_finish_customer_revision_count` CHECK (`item_count` >= 1),
  CONSTRAINT `chk_finish_customer_revision_total` CHECK (`customer_total_weight` IS NULL OR `customer_total_weight` > 0),
  CONSTRAINT `chk_finish_customer_revision_deleted` CHECK (`is_deleted` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Versioned customer-facing finish specifications';

CREATE TABLE IF NOT EXISTS `biz_finish_customer_revision_item` (
  `uuid` VARCHAR(36) NOT NULL,
  `revision_uuid` VARCHAR(36) NOT NULL,
  `finish_uuid` VARCHAR(36) NOT NULL,
  `physical_paper_name` VARCHAR(100) NOT NULL,
  `physical_gram_weight` INT NOT NULL,
  `physical_finish_width` INT NOT NULL,
  `physical_weight_snapshot` DECIMAL(12,3) DEFAULT NULL,
  `customer_paper_name` VARCHAR(100) NOT NULL,
  `customer_gram_weight` INT NOT NULL,
  `customer_finish_width` INT NOT NULL,
  `customer_display_weight` DECIMAL(12,3) DEFAULT NULL,
  `calculation_mode` VARCHAR(16) NOT NULL,
  `formula_expression` VARCHAR(500) DEFAULT NULL,
  `formula_inputs` JSON DEFAULT NULL,
  `rounding_scale` TINYINT NOT NULL DEFAULT 3,
  `rounding_mode` VARCHAR(16) NOT NULL DEFAULT 'HALF_UP',
  `zero_policy` VARCHAR(16) NOT NULL DEFAULT 'SKIP',
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
  UNIQUE KEY `uk_finish_customer_revision_item` (`revision_uuid`, `finish_uuid`),
  KEY `idx_finish_customer_revision_item_finish` (`finish_uuid`, `revision_uuid`),
  CONSTRAINT `fk_finish_customer_revision_item_revision` FOREIGN KEY (`revision_uuid`)
    REFERENCES `biz_finish_customer_revision` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_finish_customer_revision_item_finish` FOREIGN KEY (`finish_uuid`)
    REFERENCES `biz_finish_roll` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_finish_customer_item_physical` CHECK (`physical_gram_weight` > 0 AND `physical_finish_width` > 0),
  CONSTRAINT `chk_finish_customer_item_customer` CHECK (`customer_gram_weight` > 0 AND `customer_finish_width` > 0),
  CONSTRAINT `chk_finish_customer_item_weight` CHECK (`customer_display_weight` IS NULL OR `customer_display_weight` > 0),
  CONSTRAINT `chk_finish_customer_item_mode` CHECK (`calculation_mode` IN ('KEEP','FIXED','DELTA','RATIO','FORMULA','MANUAL')),
  CONSTRAINT `chk_finish_customer_item_rounding` CHECK (`rounding_scale` BETWEEN 0 AND 3 AND `rounding_mode` IN ('HALF_UP','UP','DOWN')),
  CONSTRAINT `chk_finish_customer_item_zero` CHECK (`zero_policy` IN ('SKIP','ERROR','USE_ZERO')),
  CONSTRAINT `chk_finish_customer_item_deleted` CHECK (`is_deleted` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Customer-facing finish values per revision';
