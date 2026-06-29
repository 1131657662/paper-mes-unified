ALTER TABLE `biz_process_order`
  ADD COLUMN `settle_type` TINYINT NOT NULL DEFAULT 2 COMMENT '1次结 2月结，本单结算方式快照/覆盖' AFTER `is_invoice`,
  ADD COLUMN `settle_day` TINYINT DEFAULT NULL COMMENT '月结对账日，本单快照/覆盖' AFTER `settle_type`;

UPDATE `biz_process_order` o
LEFT JOIN `sys_customer` c ON c.`uuid` = o.`customer_uuid`
SET
  o.`settle_type` = COALESCE(c.`settle_type`, o.`settle_type`, 2),
  o.`settle_day` = c.`settle_day`,
  o.`is_invoice` = COALESCE(o.`is_invoice`, c.`default_invoice`, 2),
  o.`tax_rate` = COALESCE(o.`tax_rate`, c.`tax_rate`, 0)
WHERE o.`is_deleted` = 0;
