ALTER TABLE `biz_settle_discount_approval`
  ADD COLUMN `cash_amount` DECIMAL(12,2) DEFAULT NULL AFTER `request_id`,
  ADD COLUMN `scrap_offset_amount` DECIMAL(12,2) DEFAULT NULL AFTER `cash_amount`,
  ADD COLUMN `unreceived_snapshot` DECIMAL(12,2) DEFAULT NULL AFTER `discount_amount`,
  ADD COLUMN `discount_percent` DECIMAL(7,2) DEFAULT NULL AFTER `unreceived_snapshot`,
  ADD COLUMN `required_level` VARCHAR(16) DEFAULT NULL AFTER `discount_percent`,
  ADD COLUMN `request_hash` CHAR(64) DEFAULT NULL AFTER `required_level`,
  ADD COLUMN `decision_reason` VARCHAR(255) DEFAULT NULL AFTER `approve_time`,
  ADD COLUMN `cancel_by` VARCHAR(36) DEFAULT NULL AFTER `decision_reason`,
  ADD COLUMN `cancel_by_name` VARCHAR(50) DEFAULT NULL AFTER `cancel_by`,
  ADD COLUMN `cancel_time` DATETIME DEFAULT NULL AFTER `cancel_by_name`,
  ADD COLUMN `policy_version` VARCHAR(32) DEFAULT NULL AFTER `cancel_time`;

UPDATE `biz_settle_discount_approval` a
JOIN `biz_settle_order` s ON s.`uuid` = a.`settle_uuid`
SET a.`cash_amount` = COALESCE(a.`cash_amount`, 0),
    a.`scrap_offset_amount` = COALESCE(a.`scrap_offset_amount`, 0),
    a.`unreceived_snapshot` = COALESCE(a.`unreceived_snapshot`, s.`unreceived_amount`, a.`discount_amount`),
    a.`discount_percent` = COALESCE(a.`discount_percent`,
      CASE WHEN COALESCE(s.`unreceived_amount`, 0) > 0
        THEN ROUND(a.`discount_amount` * 100 / s.`unreceived_amount`, 2) ELSE 0 END),
    a.`required_level` = COALESCE(a.`required_level`, 'ADMIN'),
    a.`policy_version` = COALESCE(a.`policy_version`, 'legacy-v1'),
    a.`request_hash` = COALESCE(a.`request_hash`, SHA2(CONCAT_WS('|', a.`settle_uuid`, '0.00', '0.00',
      FORMAT(a.`discount_amount`, 2), FORMAT(COALESCE(s.`unreceived_amount`, a.`discount_amount`), 2), a.`reason`), 256));

-- V3.3 approvals did not record the complete receipt plan. Their cash and scrap
-- components cannot be reconstructed reliably, so active legacy approvals must
-- be submitted again under the complete-plan policy.
UPDATE `biz_settle_discount_approval`
SET `approval_status` = 6,
    `decision_reason` = 'V3.63升级：历史审批缺少完整收款方案，请重新提交'
WHERE `policy_version` = 'legacy-v1'
  AND `approval_status` IN (1, 2);

ALTER TABLE `biz_settle_discount_approval`
  MODIFY COLUMN `cash_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  MODIFY COLUMN `scrap_offset_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  MODIFY COLUMN `unreceived_snapshot` DECIMAL(12,2) NOT NULL,
  MODIFY COLUMN `discount_percent` DECIMAL(7,2) NOT NULL DEFAULT 0,
  MODIFY COLUMN `required_level` VARCHAR(16) NOT NULL,
  MODIFY COLUMN `request_hash` CHAR(64) NOT NULL,
  MODIFY COLUMN `policy_version` VARCHAR(32) NOT NULL,
  ADD COLUMN `active_settle_uuid` VARCHAR(36)
    GENERATED ALWAYS AS (CASE WHEN `approval_status` IN (1, 2) THEN `settle_uuid` ELSE NULL END) STORED,
  ADD UNIQUE KEY `uk_discount_approval_active_settle` (`active_settle_uuid`),
  ADD KEY `idx_discount_approval_inbox` (`approval_status`, `required_level`, `request_time`),
  ADD KEY `idx_discount_approval_requester` (`request_by`, `approval_status`, `request_time`),
  ADD CONSTRAINT `chk_discount_approval_status` CHECK (`approval_status` BETWEEN 1 AND 6),
  ADD CONSTRAINT `chk_discount_approval_level` CHECK (`required_level` IN ('FINANCE', 'ADMIN')),
  ADD CONSTRAINT `chk_discount_approval_components` CHECK (
    `cash_amount` >= 0 AND `scrap_offset_amount` >= 0 AND `unreceived_snapshot` >= 0
  );
