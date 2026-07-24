-- V3.42: customer-specific price options for service and packaging processes.

SET SESSION lock_wait_timeout = 5;

CREATE TABLE IF NOT EXISTS `sys_customer_process_price` (
  `uuid` VARCHAR(36) NOT NULL,
  `customer_uuid` VARCHAR(36) NOT NULL,
  `catalog_uuid` VARCHAR(36) NOT NULL,
  `billing_basis` VARCHAR(12) NOT NULL COMMENT 'PIECE按件 TON按吨 FIXED固定金额',
  `price` DECIMAL(12,2) NOT NULL,
  `is_default` TINYINT NOT NULL DEFAULT 0,
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
  `active_price_key` VARCHAR(100) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 THEN CONCAT(`customer_uuid`, ':', `catalog_uuid`, ':', `billing_basis`) ELSE NULL END) STORED,
  `active_default_key` VARCHAR(80) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 AND `is_default` = 1 THEN CONCAT(`customer_uuid`, ':', `catalog_uuid`) ELSE NULL END) STORED,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_customer_process_price_active` (`active_price_key`),
  UNIQUE KEY `uk_customer_process_price_default` (`active_default_key`),
  KEY `idx_customer_process_price_customer` (`customer_uuid`, `is_deleted`),
  KEY `idx_customer_process_price_catalog` (`catalog_uuid`, `is_deleted`),
  CONSTRAINT `fk_customer_process_price_customer` FOREIGN KEY (`customer_uuid`)
    REFERENCES `sys_customer` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_customer_process_price_catalog` FOREIGN KEY (`catalog_uuid`)
    REFERENCES `sys_process_catalog` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_customer_process_price_basis` CHECK (`billing_basis` IN ('PIECE','TON','FIXED')),
  CONSTRAINT `chk_customer_process_price_value` CHECK (`price` > 0),
  CONSTRAINT `chk_customer_process_price_default` CHECK (`is_default` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户服务工艺价格方案';
