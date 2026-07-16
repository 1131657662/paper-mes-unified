-- V3.2: preserve voided delivery/settlement documents and make receipts idempotent.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

ALTER TABLE `biz_delivery_order`
  ADD COLUMN `void_reason` VARCHAR(255) DEFAULT NULL COMMENT '作废原因' AFTER `delivery_status`,
  ADD COLUMN `void_by` VARCHAR(50) DEFAULT NULL COMMENT '作废操作人' AFTER `void_reason`,
  ADD COLUMN `void_time` DATETIME DEFAULT NULL COMMENT '作废时间' AFTER `void_by`;

ALTER TABLE `biz_settle_order`
  ADD COLUMN `void_reason` VARCHAR(255) DEFAULT NULL COMMENT '作废原因' AFTER `settle_status`,
  ADD COLUMN `void_by` VARCHAR(50) DEFAULT NULL COMMENT '作废操作人' AFTER `void_reason`,
  ADD COLUMN `void_time` DATETIME DEFAULT NULL COMMENT '作废时间' AFTER `void_by`;

ALTER TABLE `biz_receive_record`
  ADD COLUMN `request_id` VARCHAR(64) DEFAULT NULL COMMENT '客户端幂等请求号' AFTER `settle_uuid`,
  ADD UNIQUE KEY `uk_receive_settle_request` (`settle_uuid`, `request_id`);
