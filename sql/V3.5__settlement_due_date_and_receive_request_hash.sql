-- V3.5: persist the server-owned settlement due date and receipt idempotency payload hash.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

ALTER TABLE `biz_settle_order`
  ADD COLUMN `due_date` DATE DEFAULT NULL COMMENT '付款到期日' AFTER `settle_date`,
  ADD KEY `idx_settle_due_status` (`is_deleted`, `settle_status`, `due_date`, `uuid`);

UPDATE `biz_settle_order` s
LEFT JOIN `sys_customer` c
  ON c.`uuid` = s.`customer_uuid` AND c.`is_deleted` = 0
SET s.`due_date` = CASE
  WHEN s.`settle_type` = 2 AND c.`settle_type` = 2 AND c.`settle_day` IS NOT NULL
    THEN CASE
      WHEN STR_TO_DATE(CONCAT(DATE_FORMAT(COALESCE(s.`period_end`, s.`settle_date`), '%Y-%m-'),
              LPAD(LEAST(c.`settle_day`, DAY(LAST_DAY(COALESCE(s.`period_end`, s.`settle_date`)))), 2, '0')),
              '%Y-%m-%d') >= COALESCE(s.`period_end`, s.`settle_date`)
        THEN STR_TO_DATE(CONCAT(DATE_FORMAT(COALESCE(s.`period_end`, s.`settle_date`), '%Y-%m-'),
                LPAD(LEAST(c.`settle_day`, DAY(LAST_DAY(COALESCE(s.`period_end`, s.`settle_date`)))), 2, '0')),
                '%Y-%m-%d')
      ELSE DATE_ADD(DATE_FORMAT(DATE_ADD(COALESCE(s.`period_end`, s.`settle_date`), INTERVAL 1 MONTH), '%Y-%m-01'),
              INTERVAL LEAST(c.`settle_day`, DAY(LAST_DAY(DATE_ADD(COALESCE(s.`period_end`, s.`settle_date`), INTERVAL 1 MONTH)))) - 1 DAY)
    END
  ELSE s.`settle_date`
END
WHERE s.`due_date` IS NULL AND s.`settle_date` IS NOT NULL;

ALTER TABLE `biz_receive_record`
  ADD COLUMN `request_hash` CHAR(64) DEFAULT NULL COMMENT '收款请求载荷SHA-256' AFTER `request_id`;
