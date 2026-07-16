-- V3.1: record discounts and small-balance write-offs separately from cash receipts.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

ALTER TABLE `biz_settle_order`
  ADD COLUMN `discount_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00
    COMMENT '优惠及尾差核销金额' AFTER `scrap_offset_amount`,
  ADD CONSTRAINT `chk_settle_discount_nonnegative` CHECK (`discount_amount` >= 0);

ALTER TABLE `biz_receive_record`
  ADD COLUMN `discount_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00
    COMMENT '优惠及尾差核销金额' AFTER `scrap_offset_amount`,
  ADD CONSTRAINT `chk_receive_discount_nonnegative` CHECK (`discount_amount` >= 0);
