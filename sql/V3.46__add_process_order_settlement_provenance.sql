SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

ALTER TABLE `biz_process_order`
  ADD COLUMN `settle_source` VARCHAR(16) DEFAULT NULL COMMENT 'INHERIT or OVERRIDE' AFTER `settle_day`,
  ADD COLUMN `settle_customer_version` INT DEFAULT NULL COMMENT 'Customer version used by settlement snapshot' AFTER `settle_source`,
  ADD COLUMN `settle_override_reason` VARCHAR(200) DEFAULT NULL COMMENT 'Reason for order settlement override' AFTER `settle_customer_version`,
  ADD CONSTRAINT `chk_order_settle_source`
    CHECK (`settle_source` IS NULL OR `settle_source` IN ('INHERIT', 'OVERRIDE')),
  ADD CONSTRAINT `chk_order_settle_customer_version`
    CHECK (`settle_source` IS NULL OR `settle_customer_version` IS NOT NULL),
  ADD CONSTRAINT `chk_order_settle_override_reason`
    CHECK (`settle_source` IS NULL
      OR (`settle_source` = 'INHERIT' AND `settle_override_reason` IS NULL)
      OR (`settle_source` = 'OVERRIDE'
        AND NULLIF(TRIM(`settle_override_reason`), '') IS NOT NULL));

-- Historical rows deliberately remain NULL because their original inherit/override intent is unknowable.
