SET SESSION lock_wait_timeout = 5;

ALTER TABLE `biz_settle_order`
  ADD COLUMN `reminder_count` INT NOT NULL DEFAULT 0 COMMENT '催收提醒次数' AFTER `unreceived_amount`,
  ADD COLUMN `last_reminder_time` DATETIME DEFAULT NULL COMMENT '最近催收时间' AFTER `reminder_count`,
  ADD COLUMN `last_reminder_by` VARCHAR(50) DEFAULT NULL COMMENT '最近催收人' AFTER `last_reminder_time`,
  ADD COLUMN `last_reminder_result` TINYINT DEFAULT NULL COMMENT '最近催收结果' AFTER `last_reminder_by`,
  ADD COLUMN `next_follow_up_date` DATE DEFAULT NULL COMMENT '下次跟进日期' AFTER `last_reminder_result`,
  ADD INDEX `idx_settle_collection_queue`
    (`is_deleted`, `settle_status`, `last_reminder_time`, `due_date`, `uuid`);

CREATE TABLE `biz_settle_collection_reminder` (
  `uuid` VARCHAR(36) NOT NULL COMMENT '催收记录主键',
  `settle_uuid` VARCHAR(36) NOT NULL COMMENT '关联结算单',
  `request_id` VARCHAR(64) NOT NULL COMMENT '客户端幂等请求号',
  `reminder_channel` TINYINT NOT NULL COMMENT '1电话 2微信 3短信 4上门 5其他',
  `reminder_result` TINYINT NOT NULL COMMENT '1已联系 2未接通 3承诺付款 4有异议 5其他',
  `contact_name` VARCHAR(100) DEFAULT NULL COMMENT '联系人',
  `reminder_time` DATETIME NOT NULL COMMENT '提醒时间',
  `next_follow_up_date` DATE DEFAULT NULL COMMENT '下次跟进日期',
  `operator_uuid` VARCHAR(36) NOT NULL COMMENT '操作人账号主键',
  `operator_name` VARCHAR(50) NOT NULL COMMENT '操作人姓名快照',
  `remark` VARCHAR(500) NOT NULL COMMENT '提醒结果说明',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1删除',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `ext_str1` VARCHAR(255) DEFAULT NULL COMMENT '扩展文本1',
  `ext_str2` VARCHAR(255) DEFAULT NULL COMMENT '扩展文本2',
  `ext_num1` DECIMAL(12,3) DEFAULT NULL COMMENT '扩展数值1',
  `ext_num2` DECIMAL(12,3) DEFAULT NULL COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_settle_collection_request` (`settle_uuid`, `request_id`),
  KEY `idx_settle_collection_time` (`settle_uuid`, `reminder_time`, `uuid`),
  KEY `idx_settle_collection_follow_up` (`next_follow_up_date`, `settle_uuid`),
  CONSTRAINT `fk_settle_collection_order` FOREIGN KEY (`settle_uuid`)
    REFERENCES `biz_settle_order` (`uuid`) ON DELETE RESTRICT,
  CONSTRAINT `chk_settle_collection_channel` CHECK (`reminder_channel` BETWEEN 1 AND 5),
  CONSTRAINT `chk_settle_collection_result` CHECK (`reminder_result` BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算催收提醒流水';
