-- V3.74: customer-confirmed remain transfer and own-inventory foundation.
-- This migration is intentionally limited to the registration/ownership slice.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

ALTER TABLE `biz_finish_roll`
    ADD COLUMN `ownership_status` TINYINT NOT NULL DEFAULT 0
        COMMENT '0客户所有 1客户/我方分属 2我方所有' AFTER `is_remain`,
    ADD COLUMN `remain_own_weight` DECIMAL(10,3) NOT NULL DEFAULT 0.000
        COMMENT '已转入我方的系统重量kg' AFTER `remaining_weight`,
    ADD COLUMN `remain_transfer_state` TINYINT NOT NULL DEFAULT 0
        COMMENT '0未转让 1部分转让 2全部转让 3部分恢复' AFTER `ownership_status`,
    ADD KEY `idx_finish_ownership` (`is_remain`, `ownership_status`, `finish_status`, `is_deleted`),
    ADD CONSTRAINT `chk_finish_ownership_status`
        CHECK (`ownership_status` IN (0, 1, 2)),
    ADD CONSTRAINT `chk_finish_remain_own_weight`
        CHECK (`remain_own_weight` >= 0);

CREATE TABLE IF NOT EXISTS `biz_remain_registration` (
    `uuid` VARCHAR(36) NOT NULL COMMENT '登记单主键',
    `registration_no` VARCHAR(50) NOT NULL COMMENT '登记单号',
    `request_id` VARCHAR(64) NOT NULL COMMENT '登记请求幂等号',
    `request_hash` CHAR(64) NOT NULL COMMENT '登记请求载荷摘要',
    `order_uuid` VARCHAR(36) NOT NULL COMMENT '来源加工单',
    `customer_uuid` VARCHAR(36) NOT NULL COMMENT '来源客户',
    `registration_date` DATETIME NOT NULL COMMENT '登记时间',
    `confirmation_name` VARCHAR(100) NOT NULL COMMENT '客户确认人',
    `confirmation_channel` VARCHAR(32) NOT NULL COMMENT '确认渠道',
    `confirmation_at` DATETIME NOT NULL COMMENT '客户确认时间',
    `confirmation_evidence` VARCHAR(500) NOT NULL COMMENT '凭证或人工核验说明',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PARTIAL_ROLLED_BACK/FULL_ROLLED_BACK',
    `price_status` VARCHAR(32) NOT NULL DEFAULT 'PRICE_PENDING' COMMENT 'PRICE_PENDING/CONFIRMED/VOIDED',
    `price_version` INT NOT NULL DEFAULT 0,
    `pricing_basis` VARCHAR(32) DEFAULT NULL,
    `price_confirmed_at` DATETIME DEFAULT NULL,
    `price_confirmed_by` VARCHAR(50) DEFAULT NULL,
    `total_transferred_weight` DECIMAL(12,3) NOT NULL DEFAULT 0.000 COMMENT '本次转入系统重量kg',
    `total_rolled_back_weight` DECIMAL(12,3) NOT NULL DEFAULT 0.000 COMMENT '已回滚系统重量kg',
    `total_processed_weight` DECIMAL(12,3) NOT NULL DEFAULT 0.000 COMMENT '已处理系统重量kg',
    `total_amount` DECIMAL(12,0) NOT NULL DEFAULT 0 COMMENT '登记单金额，整数元',
    `remark` VARCHAR(500) DEFAULT NULL,
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
    UNIQUE KEY `uk_remain_registration_no` (`registration_no`),
    UNIQUE KEY `uk_remain_registration_request` (`request_id`),
    KEY `idx_remain_registration_order` (`order_uuid`, `registration_date`, `uuid`),
    KEY `idx_remain_registration_customer` (`customer_uuid`, `status`, `registration_date`),
    CONSTRAINT `fk_remain_registration_order` FOREIGN KEY (`order_uuid`)
        REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_registration_customer` FOREIGN KEY (`customer_uuid`)
        REFERENCES `sys_customer` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_registration_status` CHECK
        (`status` IN ('ACTIVE', 'PARTIAL_ROLLED_BACK', 'FULL_ROLLED_BACK')),
    CONSTRAINT `chk_remain_registration_price_status` CHECK
        (`price_status` IN ('PRICE_PENDING', 'CONFIRMED', 'VOIDED')),
    CONSTRAINT `chk_remain_registration_weight` CHECK
        (`total_transferred_weight` > 0
         AND `total_rolled_back_weight` >= 0
         AND `total_processed_weight` >= 0
         AND `total_rolled_back_weight` + `total_processed_weight` <= `total_transferred_weight`),
    CONSTRAINT `chk_remain_registration_amount` CHECK (`total_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余料抵扣登记单';

CREATE TABLE IF NOT EXISTS `biz_remain_registration_line` (
    `uuid` VARCHAR(36) NOT NULL COMMENT '登记明细主键',
    `registration_uuid` VARCHAR(36) NOT NULL,
    `source_finish_roll_uuid` VARCHAR(36) NOT NULL COMMENT '来源余卷',
    `source_order_uuid` VARCHAR(36) NOT NULL,
    `source_customer_uuid` VARCHAR(36) NOT NULL,
    `source_system_weight` DECIMAL(12,3) NOT NULL COMMENT '登记前客户可用重量kg',
    `transferred_system_weight` DECIMAL(12,3) NOT NULL COMMENT '本次转入我方重量kg',
    `rolled_back_system_weight` DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    `processed_system_weight` DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    `current_own_weight` DECIMAL(12,3) NOT NULL COMMENT '当前我方库存重量kg',
    `amount` DECIMAL(12,0) NOT NULL DEFAULT 0 COMMENT '明细金额，整数元',
    `applied_amount` DECIMAL(12,0) NOT NULL DEFAULT 0 COMMENT '已进入结算分配的金额，整数元',
    `applied_weight` DECIMAL(12,3) NOT NULL DEFAULT 0.000 COMMENT '已进入结算分配的重量kg',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PARTIAL_ROLLED_BACK/FULL_ROLLED_BACK',
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
    `active_source_finish_roll_uuid` VARCHAR(36)
        GENERATED ALWAYS AS (CASE
            WHEN `is_deleted` = 0 AND `status` <> 'FULL_ROLLED_BACK'
            THEN `source_finish_roll_uuid` ELSE NULL END) STORED,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_remain_line_active_source` (`active_source_finish_roll_uuid`),
    KEY `idx_remain_line_registration` (`registration_uuid`, `status`),
    KEY `idx_remain_line_source` (`source_finish_roll_uuid`),
    CONSTRAINT `fk_remain_line_registration` FOREIGN KEY (`registration_uuid`)
        REFERENCES `biz_remain_registration` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_line_finish_roll` FOREIGN KEY (`source_finish_roll_uuid`)
        REFERENCES `biz_finish_roll` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_line_order` FOREIGN KEY (`source_order_uuid`)
        REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_line_customer` FOREIGN KEY (`source_customer_uuid`)
        REFERENCES `sys_customer` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_line_status` CHECK
        (`status` IN ('ACTIVE', 'PARTIAL_ROLLED_BACK', 'FULL_ROLLED_BACK')),
    CONSTRAINT `chk_remain_line_weights` CHECK
        (`source_system_weight` > 0
         AND `transferred_system_weight` > 0
         AND `rolled_back_system_weight` >= 0
         AND `processed_system_weight` >= 0
         AND `current_own_weight` >= 0
         AND `applied_amount` >= 0
         AND `applied_weight` >= 0
         AND `rolled_back_system_weight` + `processed_system_weight` + `current_own_weight`
             = `transferred_system_weight`),
    CONSTRAINT `chk_remain_line_amount` CHECK (`amount` >= 0 AND `applied_amount` <= `amount`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余料抵扣登记明细';

CREATE TABLE IF NOT EXISTS `biz_remain_price_version` (
    `uuid` VARCHAR(36) NOT NULL,
    `registration_uuid` VARCHAR(36) NOT NULL,
    `version_no` INT NOT NULL,
    `pricing_basis` VARCHAR(32) NOT NULL,
    `total_amount` DECIMAL(12,0) NOT NULL COMMENT '确认金额，整数元',
    `request_id` VARCHAR(64) NOT NULL,
    `request_hash` CHAR(64) NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'CONFIRMED',
    `confirmed_at` DATETIME NOT NULL,
    `confirmed_by` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_remain_price_version` (`registration_uuid`, `version_no`),
    UNIQUE KEY `uk_remain_price_request` (`request_id`),
    CONSTRAINT `fk_remain_price_registration` FOREIGN KEY (`registration_uuid`)
        REFERENCES `biz_remain_registration` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_price_amount` CHECK (`total_amount` >= 0),
    CONSTRAINT `chk_remain_price_status` CHECK (`status` IN ('CONFIRMED', 'VOIDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余料价格确认版本';

CREATE TABLE IF NOT EXISTS `biz_remain_application` (
    `uuid` VARCHAR(36) NOT NULL,
    `registration_uuid` VARCHAR(36) NOT NULL,
    `settle_uuid` VARCHAR(36) NOT NULL,
    `adjustment_uuid` VARCHAR(36) DEFAULT NULL COMMENT '来源待调整余额',
    `receive_uuid` VARCHAR(36) DEFAULT NULL,
    `customer_uuid` VARCHAR(36) NOT NULL,
    `application_type` VARCHAR(16) NOT NULL DEFAULT 'APPLY',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    `amount` DECIMAL(12,0) NOT NULL COMMENT '抵扣金额，整数元',
    `weight` DECIMAL(12,3) NOT NULL COMMENT '抵扣系统重量kg',
    `request_id` VARCHAR(64) NOT NULL,
    `request_hash` CHAR(64) NOT NULL,
    `reversal_of_uuid` VARCHAR(36) DEFAULT NULL,
    `create_by` VARCHAR(50) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(50) DEFAULT NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 1,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `active_registration_settle` VARCHAR(73)
        GENERATED ALWAYS AS (CASE WHEN `status` = 'ACTIVE' AND `application_type` = 'APPLY' AND `is_deleted` = 0
            THEN CONCAT(`registration_uuid`, ':', `settle_uuid`) ELSE NULL END) STORED,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_remain_application_active_target` (`active_registration_settle`),
    UNIQUE KEY `uk_remain_application_request` (`request_id`),
    KEY `idx_remain_application_registration` (`registration_uuid`, `status`),
    KEY `idx_remain_application_settle` (`settle_uuid`, `status`),
    CONSTRAINT `fk_remain_application_registration` FOREIGN KEY (`registration_uuid`)
        REFERENCES `biz_remain_registration` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_application_settle` FOREIGN KEY (`settle_uuid`)
        REFERENCES `biz_settle_order` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_application_customer` FOREIGN KEY (`customer_uuid`)
        REFERENCES `sys_customer` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_application_type` CHECK (`application_type` IN ('APPLY', 'REVERSE')),
    CONSTRAINT `chk_remain_application_status` CHECK (`status` IN ('ACTIVE', 'REVERSED')),
    CONSTRAINT `chk_remain_application_amount` CHECK (`amount` > 0 AND `weight` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余料抵扣应用及反向关系';

CREATE TABLE IF NOT EXISTS `biz_remain_application_line` (
    `uuid` VARCHAR(36) NOT NULL,
    `application_uuid` VARCHAR(36) NOT NULL,
    `registration_line_uuid` VARCHAR(36) NOT NULL,
    `amount` DECIMAL(12,0) NOT NULL,
    `weight` DECIMAL(12,3) NOT NULL,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_remain_application_line` (`application_uuid`, `registration_line_uuid`),
    CONSTRAINT `fk_remain_application_line_application` FOREIGN KEY (`application_uuid`)
        REFERENCES `biz_remain_application` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_application_line_registration_line` FOREIGN KEY (`registration_line_uuid`)
        REFERENCES `biz_remain_registration_line` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_application_line_values` CHECK (`amount` >= 0 AND `weight` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余料抵扣应用来源明细';

CREATE TABLE IF NOT EXISTS `biz_remain_adjustment` (
    `uuid` VARCHAR(36) NOT NULL COMMENT '余料结算调整主键',
    `adjustment_no` VARCHAR(50) NOT NULL,
    `request_id` VARCHAR(64) NOT NULL,
    `request_hash` CHAR(64) NOT NULL,
    `registration_uuid` VARCHAR(36) NOT NULL,
    `source_settle_uuid` VARCHAR(36) DEFAULT NULL,
    `target_settle_uuid` VARCHAR(36) DEFAULT NULL,
    `customer_uuid` VARCHAR(36) NOT NULL,
    `target_type` VARCHAR(24) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/NEXT_SETTLEMENT/CUSTOMER_CREDIT/REFUND',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPLIED/REVERSED/CANCELLED',
    `amount` DECIMAL(12,0) NOT NULL COMMENT '调整金额，整数元',
    `weight` DECIMAL(12,3) NOT NULL COMMENT '调整系统重量kg',
    `reason` VARCHAR(500) DEFAULT NULL,
    `create_by` VARCHAR(50) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(50) DEFAULT NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 1,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_remain_adjustment_no` (`adjustment_no`),
    UNIQUE KEY `uk_remain_adjustment_request` (`request_id`),
    KEY `idx_remain_adjustment_registration` (`registration_uuid`, `status`),
    KEY `idx_remain_adjustment_target` (`target_type`, `status`, `customer_uuid`),
    CONSTRAINT `fk_remain_adjustment_registration` FOREIGN KEY (`registration_uuid`)
        REFERENCES `biz_remain_registration` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_adjustment_source_settle` FOREIGN KEY (`source_settle_uuid`)
        REFERENCES `biz_settle_order` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_adjustment_target_settle` FOREIGN KEY (`target_settle_uuid`)
        REFERENCES `biz_settle_order` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_adjustment_customer` FOREIGN KEY (`customer_uuid`)
        REFERENCES `sys_customer` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_adjustment_target` CHECK (`target_type` IN ('PENDING', 'NEXT_SETTLEMENT', 'CUSTOMER_CREDIT', 'REFUND')),
    CONSTRAINT `chk_remain_adjustment_status` CHECK (`status` IN ('PENDING', 'APPLIED', 'REVERSED', 'CANCELLED')),
    CONSTRAINT `chk_remain_adjustment_values` CHECK (`amount` > 0 AND `weight` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余料抵扣结算调整';

CREATE TABLE IF NOT EXISTS `biz_remain_adjustment_line` (
    `uuid` VARCHAR(36) NOT NULL,
    `adjustment_uuid` VARCHAR(36) NOT NULL,
    `registration_line_uuid` VARCHAR(36) NOT NULL,
    `amount` DECIMAL(12,0) NOT NULL,
    `weight` DECIMAL(12,3) NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_remain_adjustment_line` (`adjustment_uuid`, `registration_line_uuid`),
    CONSTRAINT `fk_remain_adjustment_line_adjustment` FOREIGN KEY (`adjustment_uuid`)
        REFERENCES `biz_remain_adjustment` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_adjustment_line_registration_line` FOREIGN KEY (`registration_line_uuid`)
        REFERENCES `biz_remain_registration_line` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_adjustment_line_values` CHECK (`amount` >= 0 AND `weight` >= 0
        AND (`amount` > 0 OR `weight` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余料结算调整来源明细';

CREATE TABLE IF NOT EXISTS `biz_remain_customer_credit_account` (
    `uuid` VARCHAR(36) NOT NULL,
    `customer_uuid` VARCHAR(36) NOT NULL,
    `current_amount` DECIMAL(12,0) NOT NULL DEFAULT 0 COMMENT '客户余款余额，整数元',
    `last_ledger_uuid` VARCHAR(36) DEFAULT NULL,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `create_by` VARCHAR(50) DEFAULT NULL,
    `update_by` VARCHAR(50) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 1,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_remain_credit_account_customer` (`customer_uuid`),
    CONSTRAINT `fk_remain_credit_account_customer` FOREIGN KEY (`customer_uuid`)
        REFERENCES `sys_customer` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_credit_account_amount` CHECK (`current_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户余料抵扣余款账户';

CREATE TABLE IF NOT EXISTS `biz_remain_customer_credit_ledger` (
    `uuid` VARCHAR(36) NOT NULL,
    `account_uuid` VARCHAR(36) NOT NULL,
    `adjustment_uuid` VARCHAR(36) NOT NULL,
    `customer_uuid` VARCHAR(36) NOT NULL,
    `event_type` VARCHAR(16) NOT NULL COMMENT 'CREDIT/REVERSE',
    `amount` DECIMAL(12,0) NOT NULL COMMENT '余额变动金额，整数元',
    `weight` DECIMAL(12,3) NOT NULL COMMENT '来源系统重量kg',
    `before_amount` DECIMAL(12,0) NOT NULL,
    `after_amount` DECIMAL(12,0) NOT NULL,
    `request_id` VARCHAR(64) NOT NULL,
    `reversal_of_uuid` VARCHAR(36) DEFAULT NULL,
    `create_by` VARCHAR(50) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_remain_credit_ledger_request` (`request_id`),
    UNIQUE KEY `uk_remain_credit_ledger_reversal` (`reversal_of_uuid`),
    KEY `idx_remain_credit_ledger_account` (`account_uuid`, `create_time`, `uuid`),
    KEY `idx_remain_credit_ledger_adjustment` (`adjustment_uuid`, `event_type`),
    CONSTRAINT `fk_remain_credit_ledger_account` FOREIGN KEY (`account_uuid`)
        REFERENCES `biz_remain_customer_credit_account` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_credit_ledger_adjustment` FOREIGN KEY (`adjustment_uuid`)
        REFERENCES `biz_remain_adjustment` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_credit_ledger_customer` FOREIGN KEY (`customer_uuid`)
        REFERENCES `sys_customer` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_credit_ledger_reversal` FOREIGN KEY (`reversal_of_uuid`)
        REFERENCES `biz_remain_customer_credit_ledger` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_credit_ledger_event` CHECK (`event_type` IN ('CREDIT', 'REVERSE')),
    CONSTRAINT `chk_remain_credit_ledger_values` CHECK
        (`amount` > 0 AND `weight` > 0 AND `before_amount` >= 0 AND `after_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户余款不可变流水';

CREATE TABLE IF NOT EXISTS `biz_remain_refund` (
    `uuid` VARCHAR(36) NOT NULL,
    `refund_no` VARCHAR(50) NOT NULL,
    `adjustment_uuid` VARCHAR(36) NOT NULL,
    `customer_uuid` VARCHAR(36) NOT NULL,
    `amount` DECIMAL(12,0) NOT NULL COMMENT '退款金额，整数元',
    `weight` DECIMAL(12,3) NOT NULL COMMENT '来源系统重量kg',
    `status` VARCHAR(16) NOT NULL DEFAULT 'REQUESTED' COMMENT 'REQUESTED/APPROVED/PAID/CANCELLED',
    `request_id` VARCHAR(64) NOT NULL,
    `request_hash` CHAR(64) NOT NULL,
    `approve_request_id` VARCHAR(64) DEFAULT NULL,
    `pay_request_id` VARCHAR(64) DEFAULT NULL,
    `cancel_request_id` VARCHAR(64) DEFAULT NULL,
    `payment_reference` VARCHAR(100) DEFAULT NULL,
    `reason` VARCHAR(500) DEFAULT NULL,
    `approved_by` VARCHAR(50) DEFAULT NULL,
    `approved_at` DATETIME DEFAULT NULL,
    `paid_by` VARCHAR(50) DEFAULT NULL,
    `paid_at` DATETIME DEFAULT NULL,
    `create_by` VARCHAR(50) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(50) DEFAULT NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 1,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_remain_refund_no` (`refund_no`),
    UNIQUE KEY `uk_remain_refund_request` (`request_id`),
    KEY `idx_remain_refund_adjustment` (`adjustment_uuid`, `status`),
    KEY `idx_remain_refund_customer` (`customer_uuid`, `status`, `create_time`),
    CONSTRAINT `fk_remain_refund_adjustment` FOREIGN KEY (`adjustment_uuid`)
        REFERENCES `biz_remain_adjustment` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_refund_customer` FOREIGN KEY (`customer_uuid`)
        REFERENCES `sys_customer` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_refund_status` CHECK (`status` IN ('REQUESTED', 'APPROVED', 'PAID', 'CANCELLED')),
    CONSTRAINT `chk_remain_refund_values` CHECK (`amount` > 0 AND `weight` > 0),
    CONSTRAINT `chk_remain_refund_payment` CHECK
        ((`status` = 'PAID' AND `payment_reference` IS NOT NULL AND `paid_at` IS NOT NULL)
         OR (`status` <> 'PAID'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余料退款申请与支付事实';

ALTER TABLE `biz_remain_application`
    ADD CONSTRAINT `fk_remain_application_adjustment` FOREIGN KEY (`adjustment_uuid`)
        REFERENCES `biz_remain_adjustment` (`uuid`) ON DELETE RESTRICT;

ALTER TABLE `biz_receive_record`
    ADD COLUMN `source_type` VARCHAR(32) NOT NULL DEFAULT 'LEGACY'
        COMMENT 'LEGACY/CASH/DISCOUNT/REMAIN_OFFSET' AFTER `receive_type`,
    ADD COLUMN `remain_application_uuid` VARCHAR(36) DEFAULT NULL
        COMMENT '余料抵扣应用来源' AFTER `source_type`,
    ADD KEY `idx_receive_source_type` (`source_type`, `record_status`),
    ADD KEY `idx_receive_remain_application` (`remain_application_uuid`);

CREATE TABLE IF NOT EXISTS `biz_remain_inventory_lot` (
    `uuid` VARCHAR(36) NOT NULL COMMENT '我方余料库存批次主键',
    `registration_line_uuid` VARCHAR(36) NOT NULL,
    `source_finish_roll_uuid` VARCHAR(36) NOT NULL,
    `customer_uuid` VARCHAR(36) NOT NULL,
    `warehouse_uuid` VARCHAR(36) DEFAULT NULL,
    `current_weight` DECIMAL(12,3) NOT NULL COMMENT '当前我方库存重量kg',
    `status` VARCHAR(32) NOT NULL DEFAULT 'IN_OWN_STOCK' COMMENT 'IN_OWN_STOCK/EMPTY/VOIDED',
    `price_status` VARCHAR(32) NOT NULL DEFAULT 'PRICE_PENDING',
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
    UNIQUE KEY `uk_remain_lot_line` (`registration_line_uuid`),
    KEY `idx_remain_lot_available` (`is_deleted`, `status`, `price_status`, `current_weight`),
    CONSTRAINT `fk_remain_lot_line` FOREIGN KEY (`registration_line_uuid`)
        REFERENCES `biz_remain_registration_line` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_lot_finish_roll` FOREIGN KEY (`source_finish_roll_uuid`)
        REFERENCES `biz_finish_roll` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_lot_customer` FOREIGN KEY (`customer_uuid`)
        REFERENCES `sys_customer` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_lot_weight` CHECK (`current_weight` >= 0),
    CONSTRAINT `chk_remain_lot_status` CHECK (`status` IN ('IN_OWN_STOCK', 'EMPTY', 'VOIDED')),
    CONSTRAINT `chk_remain_lot_price_status` CHECK
        (`price_status` IN ('PRICE_PENDING', 'CONFIRMED', 'VOIDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='我方余料库存批次';

CREATE TABLE IF NOT EXISTS `biz_remain_inventory_ledger` (
    `uuid` VARCHAR(36) NOT NULL COMMENT '余料库存流水主键',
    `lot_uuid` VARCHAR(36) NOT NULL,
    `registration_line_uuid` VARCHAR(36) NOT NULL,
    `source_finish_roll_uuid` VARCHAR(36) NOT NULL,
    `event_type` VARCHAR(32) NOT NULL COMMENT 'TRANSFER_IN/ROLLBACK',
    `weight_delta` DECIMAL(12,3) NOT NULL,
    `before_weight` DECIMAL(12,3) NOT NULL,
    `after_weight` DECIMAL(12,3) NOT NULL,
    `request_id` VARCHAR(64) NOT NULL,
    `reason` VARCHAR(500) DEFAULT NULL,
    `reversal_of_uuid` VARCHAR(36) DEFAULT NULL,
    `create_by` VARCHAR(50) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_remain_ledger_request` (`request_id`),
    KEY `idx_remain_ledger_lot` (`lot_uuid`, `create_time`, `uuid`),
    CONSTRAINT `fk_remain_ledger_lot` FOREIGN KEY (`lot_uuid`)
        REFERENCES `biz_remain_inventory_lot` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_ledger_line` FOREIGN KEY (`registration_line_uuid`)
        REFERENCES `biz_remain_registration_line` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_ledger_finish_roll` FOREIGN KEY (`source_finish_roll_uuid`)
        REFERENCES `biz_finish_roll` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_ledger_reversal` FOREIGN KEY (`reversal_of_uuid`)
        REFERENCES `biz_remain_inventory_ledger` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_ledger_event` CHECK (`event_type` IN ('TRANSFER_IN', 'ROLLBACK', 'SALE_OUT', 'SALE_REVERSAL')),
    CONSTRAINT `chk_remain_ledger_weight` CHECK
        (`weight_delta` <> 0 AND `before_weight` >= 0 AND `after_weight` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='我方余料库存不可变流水';

CREATE TABLE IF NOT EXISTS `biz_remain_sale` (
    `uuid` VARCHAR(36) NOT NULL COMMENT '我方余料处理单主键',
    `sale_no` VARCHAR(50) NOT NULL COMMENT '处理单号',
    `request_id` VARCHAR(64) NOT NULL COMMENT '处理请求幂等号',
    `request_hash` CHAR(64) NOT NULL COMMENT '处理请求摘要',
    `sale_kind` VARCHAR(16) NOT NULL DEFAULT 'SALE' COMMENT 'SALE/REVERSAL',
    `reversal_of_uuid` VARCHAR(36) DEFAULT NULL COMMENT '被撤销处理单',
    `process_date` DATETIME NOT NULL,
    `warehouse_uuid` VARCHAR(36) DEFAULT NULL,
    `pricing_mode` VARCHAR(32) NOT NULL COMMENT 'SYSTEM_WEIGHT_UNIT_PRICE/ACTUAL_WEIGHT_UNIT_PRICE/TOTAL_AMOUNT',
    `system_weight` DECIMAL(12,3) NOT NULL COMMENT '系统扣减总重量kg',
    `actual_weight` DECIMAL(12,3) DEFAULT NULL COMMENT '整车实际过磅总重量kg',
    `unit_price` DECIMAL(12,0) DEFAULT NULL COMMENT '整数元单价',
    `calculated_amount` DECIMAL(12,0) NOT NULL DEFAULT 0 COMMENT '计算金额，整数元',
    `received_amount` DECIMAL(12,0) NOT NULL DEFAULT 0 COMMENT '最终实收金额，整数元',
    `buyer_name` VARCHAR(100) DEFAULT NULL,
    `vehicle_no` VARCHAR(50) DEFAULT NULL,
    `weighing_ticket_no` VARCHAR(100) DEFAULT NULL,
    `weighing_evidence` VARCHAR(500) DEFAULT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'CONFIRMED' COMMENT 'CONFIRMED/VOIDED',
    `reason` VARCHAR(500) DEFAULT NULL,
    `create_by` VARCHAR(50) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(50) DEFAULT NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 1,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_remain_sale_no` (`sale_no`),
    UNIQUE KEY `uk_remain_sale_request` (`request_id`),
    KEY `idx_remain_sale_status_date` (`status`, `process_date`),
    CONSTRAINT `fk_remain_sale_reversal` FOREIGN KEY (`reversal_of_uuid`)
        REFERENCES `biz_remain_sale` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_sale_kind` CHECK (`sale_kind` IN ('SALE', 'REVERSAL')),
    CONSTRAINT `chk_remain_sale_status` CHECK (`status` IN ('CONFIRMED', 'VOIDED')),
    CONSTRAINT `chk_remain_sale_values` CHECK
        (`system_weight` > 0 AND `calculated_amount` >= 0 AND `received_amount` >= 0
         AND (`actual_weight` IS NULL OR `actual_weight` > 0)
         AND (`unit_price` IS NULL OR `unit_price` >= 0)),
    CONSTRAINT `chk_remain_sale_reversal` CHECK
        ((`sale_kind` = 'SALE' AND `reversal_of_uuid` IS NULL)
         OR (`sale_kind` = 'REVERSAL' AND `reversal_of_uuid` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='我方余料出售或处理单';

CREATE TABLE IF NOT EXISTS `biz_remain_sale_line` (
    `uuid` VARCHAR(36) NOT NULL,
    `sale_uuid` VARCHAR(36) NOT NULL,
    `lot_uuid` VARCHAR(36) NOT NULL,
    `registration_line_uuid` VARCHAR(36) NOT NULL,
    `system_weight` DECIMAL(12,3) NOT NULL COMMENT '本批次系统扣减重量kg',
    `amount` DECIMAL(12,0) NOT NULL DEFAULT 0 COMMENT '分摊金额，整数元',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_remain_sale_line` (`sale_uuid`, `lot_uuid`),
    KEY `idx_remain_sale_line_lot` (`lot_uuid`, `create_time`),
    CONSTRAINT `fk_remain_sale_line_sale` FOREIGN KEY (`sale_uuid`)
        REFERENCES `biz_remain_sale` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_sale_line_lot` FOREIGN KEY (`lot_uuid`)
        REFERENCES `biz_remain_inventory_lot` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_remain_sale_line_registration_line` FOREIGN KEY (`registration_line_uuid`)
        REFERENCES `biz_remain_registration_line` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `chk_remain_sale_line_values` CHECK (`system_weight` > 0 AND `amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='我方余料处理单明细';
