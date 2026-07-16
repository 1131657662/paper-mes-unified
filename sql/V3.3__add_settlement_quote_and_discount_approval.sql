-- V3.3: add settlement creation quote contracts and auditable discount approvals.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

ALTER TABLE `biz_settle_order`
  ADD COLUMN `request_id` VARCHAR(64) DEFAULT NULL COMMENT '客户端幂等请求号' AFTER `customer_name`,
  ADD COLUMN `quote_version` VARCHAR(32) DEFAULT NULL COMMENT '创建时报价算法版本' AFTER `request_id`,
  ADD COLUMN `quote_hash` CHAR(64) DEFAULT NULL COMMENT '创建时报价SHA-256' AFTER `quote_version`,
  ADD UNIQUE KEY `uk_settle_request_id` (`request_id`);

ALTER TABLE `biz_receive_record`
  ADD COLUMN `discount_reason` VARCHAR(255) DEFAULT NULL COMMENT '优惠及尾差核销原因' AFTER `discount_amount`,
  ADD COLUMN `discount_approval_uuid` VARCHAR(36) DEFAULT NULL COMMENT '超过阈值时关联审批记录' AFTER `discount_reason`,
  ADD COLUMN `discount_approved_by` VARCHAR(50) DEFAULT NULL COMMENT '优惠批准人或免审登记人' AFTER `discount_approval_uuid`;

CREATE TABLE `biz_settle_discount_approval` (
  `uuid` VARCHAR(36) NOT NULL,
  `settle_uuid` VARCHAR(36) NOT NULL,
  `request_id` VARCHAR(64) NOT NULL,
  `discount_amount` DECIMAL(12,2) NOT NULL,
  `reason` VARCHAR(255) NOT NULL,
  `approval_status` TINYINT NOT NULL DEFAULT 1,
  `request_by` VARCHAR(36) NOT NULL,
  `request_by_name` VARCHAR(50) NOT NULL,
  `request_time` DATETIME NOT NULL,
  `approve_by` VARCHAR(36) DEFAULT NULL,
  `approve_by_name` VARCHAR(50) DEFAULT NULL,
  `approve_time` DATETIME DEFAULT NULL,
  `used_receive_uuid` VARCHAR(36) DEFAULT NULL,
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
  UNIQUE KEY `uk_discount_approval_request` (`settle_uuid`, `request_id`),
  UNIQUE KEY `uk_discount_approval_receive` (`used_receive_uuid`),
  KEY `idx_discount_approval_settle_status` (`settle_uuid`, `approval_status`),
  CONSTRAINT `chk_discount_approval_amount_positive` CHECK (`discount_amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算优惠及尾差审批记录';

INSERT INTO `sys_config_item`
(`uuid`, `config_group`, `config_key`, `config_name`, `config_value`, `value_type`, `unit`, `sort_no`, `status`, `built_in`, `remark`, `create_by`, `update_by`)
VALUES
('cfg-settle-discount-auto-limit', 'settle', 'settle.discountAutoApproveLimit', '优惠免审上限', '1.00', 'number', '元', 10, 1, 1, '不超过该金额的尾差可由有权限财务直接核销', 'system', 'system'),
('cfg-settle-discount-max-amount', 'settle', 'settle.discountMaxAmount', '单次优惠金额上限', '500.00', 'number', '元', 20, 1, 1, '超过该金额禁止通过收款核销', 'system', 'system'),
('cfg-settle-discount-max-percent', 'settle', 'settle.discountMaxPercent', '单次优惠比例上限', '10.00', 'number', '%', 30, 1, 1, '优惠金额占当前未收金额的最大比例', 'system', 'system')
ON DUPLICATE KEY UPDATE `config_key` = VALUES(`config_key`);
