CREATE TABLE IF NOT EXISTS `sys_no_rule` (
  `uuid` VARCHAR(36) NOT NULL COMMENT '主键UUID',
  `biz_type` VARCHAR(50) NOT NULL COMMENT '业务类型 process_order/delivery_order/settle_order/finish_roll',
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `prefix` VARCHAR(20) NOT NULL COMMENT '单号前缀',
  `pattern_type` TINYINT NOT NULL DEFAULT 1 COMMENT '格式 1前缀+日期+序号 2前缀+序号',
  `date_pattern` VARCHAR(20) DEFAULT 'yyyyMMdd' COMMENT '日期格式 yyyyMMdd/yyyyMM/yyyy',
  `serial_length` INT NOT NULL DEFAULT 4 COMMENT '流水位数',
  `reset_cycle` TINYINT NOT NULL DEFAULT 1 COMMENT '重置周期 0不重置 1按日 2按月 3按年',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
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
  UNIQUE KEY `uk_sys_no_rule_biz` (`biz_type`, `is_deleted`),
  KEY `idx_sys_no_rule_status` (`status`),
  KEY `idx_sys_no_rule_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统单号规则配置';

INSERT INTO `sys_no_rule`
(`uuid`, `biz_type`, `rule_name`, `prefix`, `pattern_type`, `date_pattern`, `serial_length`, `reset_cycle`, `status`, `remark`)
VALUES
('no-rule-process-order', 'process_order', '加工单号', 'JG', 1, 'yyyyMMdd', 4, 1, 1, '默认加工单号：JG+日期+4位日流水'),
('no-rule-delivery-order', 'delivery_order', '出库单号', 'CK', 1, 'yyyyMMdd', 4, 1, 1, '默认出库单号：CK+日期+4位日流水'),
('no-rule-settle-order', 'settle_order', '结算单号', 'JS', 1, 'yyyyMMdd', 4, 1, 1, '默认结算单号：JS+日期+4位日流水'),
('no-rule-finish-roll', 'finish_roll', '成品卷号', 'A', 2, 'yyyyMMdd', 6, 0, 1, '默认成品卷号：前缀+6位全局流水'),
('no-rule-customer', 'customer', '客户编码', 'KH', 2, 'yyyyMMdd', 6, 0, 1, '默认客户编码：KH+6位全局流水'),
('no-rule-paper', 'paper', '纸张编码', 'ZZ', 2, 'yyyyMMdd', 6, 0, 1, '默认纸张编码：ZZ+6位全局流水'),
('no-rule-machine', 'machine', '机台编码', 'JT', 2, 'yyyyMMdd', 6, 0, 1, '默认机台编码：JT+6位全局流水'),
('no-rule-warehouse', 'warehouse', '仓库编码', 'CKD', 2, 'yyyyMMdd', 6, 0, 1, '默认仓库编码：CKD+6位全局流水')
ON DUPLICATE KEY UPDATE
  `rule_name` = VALUES(`rule_name`),
  `remark` = VALUES(`remark`);

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `biz_delivery_order` ADD COLUMN `snap_delivery` JSON DEFAULT NULL COMMENT ''出库确认快照JSON''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_delivery_order'
    AND column_name = 'snap_delivery'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `biz_delivery_order` ADD COLUMN `snap_delivery_time` DATETIME DEFAULT NULL COMMENT ''出库确认快照时间''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_delivery_order'
    AND column_name = 'snap_delivery_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `biz_settle_order` ADD COLUMN `snap_bill` JSON DEFAULT NULL COMMENT ''结算单快照JSON''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_settle_order'
    AND column_name = 'snap_bill'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `biz_settle_order` ADD COLUMN `snap_bill_time` DATETIME DEFAULT NULL COMMENT ''结算单快照时间''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_settle_order'
    AND column_name = 'snap_bill_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
