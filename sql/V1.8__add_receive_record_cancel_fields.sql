ALTER TABLE `biz_receive_record`
  ADD COLUMN `record_status` TINYINT NOT NULL DEFAULT 1 COMMENT '1有效 2已撤销' AFTER `operator`,
  ADD COLUMN `cancel_time` DATETIME DEFAULT NULL COMMENT '撤销时间' AFTER `record_status`,
  ADD COLUMN `cancel_by` VARCHAR(50) DEFAULT NULL COMMENT '撤销人' AFTER `cancel_time`,
  ADD COLUMN `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '撤销原因' AFTER `cancel_by`;

CREATE INDEX `idx_receive_record_status` ON `biz_receive_record` (`record_status`);
