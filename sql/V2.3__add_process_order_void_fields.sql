ALTER TABLE `biz_process_order`
  ADD COLUMN `void_time` DATETIME DEFAULT NULL COMMENT '作废时间' AFTER `back_record_user`,
  ADD COLUMN `void_user` VARCHAR(50) DEFAULT NULL COMMENT '作废人' AFTER `void_time`,
  ADD COLUMN `void_reason` VARCHAR(255) DEFAULT NULL COMMENT '作废原因' AFTER `void_user`;
