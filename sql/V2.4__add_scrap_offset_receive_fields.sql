ALTER TABLE `biz_receive_record`
  MODIFY COLUMN `pay_method` TINYINT DEFAULT NULL COMMENT '1现金 2转账 3微信 4支付宝；纯废纸抵扣可为空',
  ADD COLUMN `cash_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '现金/转账/微信/支付宝实际收款' AFTER `receive_amount`,
  ADD COLUMN `scrap_offset_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '废纸抵扣金额' AFTER `cash_amount`,
  ADD COLUMN `scrap_weight` DECIMAL(12,3) NOT NULL DEFAULT 0.000 COMMENT '废纸抵扣重量kg' AFTER `scrap_offset_amount`,
  ADD COLUMN `scrap_unit_price` DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '废纸抵扣折算单价' AFTER `scrap_weight`,
  ADD COLUMN `receive_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1普通收款 2废纸抵扣 3混合收款' AFTER `scrap_unit_price`;

UPDATE `biz_receive_record`
SET `cash_amount` = COALESCE(`receive_amount`, 0),
    `scrap_offset_amount` = 0.00,
    `scrap_weight` = 0.000,
    `scrap_unit_price` = 0.0000,
    `receive_type` = 1
WHERE `is_deleted` = 0;

ALTER TABLE `biz_settle_order`
  ADD COLUMN `cash_received_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '现金实收金额' AFTER `received_amount`,
  ADD COLUMN `scrap_offset_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '废纸抵扣金额' AFTER `cash_received_amount`;

UPDATE `biz_settle_order` so
LEFT JOIN (
    SELECT settle_uuid,
           SUM(CASE WHEN COALESCE(record_status, 1) = 1 THEN COALESCE(cash_amount, receive_amount, 0) ELSE 0 END) AS cash_amount,
           SUM(CASE WHEN COALESCE(record_status, 1) = 1 THEN COALESCE(scrap_offset_amount, 0) ELSE 0 END) AS scrap_offset_amount
    FROM `biz_receive_record`
    WHERE is_deleted = 0
    GROUP BY settle_uuid
) rr ON rr.settle_uuid = so.uuid
SET so.cash_received_amount = COALESCE(rr.cash_amount, 0),
    so.scrap_offset_amount = COALESCE(rr.scrap_offset_amount, 0)
WHERE so.is_deleted = 0;

CREATE INDEX `idx_receive_record_type` ON `biz_receive_record` (`receive_type`);
