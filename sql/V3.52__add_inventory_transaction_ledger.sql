-- V3.52: immutable finished-goods inventory transaction ledger.
-- The ledger starts at the inventory switch day. It must not be backfilled from
-- historical snapshots. The opening command is responsible for reconciliation.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

CREATE TABLE IF NOT EXISTS `biz_inventory_transaction` (
  `uuid`                         VARCHAR(36)   NOT NULL COMMENT '流水主键',
  `sequence_no`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '数据库写入顺序号',
  `finish_roll_uuid`             VARCHAR(36)   NOT NULL COMMENT '成品卷 UUID',
  `event_type`                   VARCHAR(32)   NOT NULL COMMENT '流水事件类型',
  `source_business_type`         VARCHAR(64)   NOT NULL COMMENT '来源业务类型',
  `source_business_uuid`         VARCHAR(36)   NOT NULL COMMENT '来源业务 UUID',
  `quantity_delta`               DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '实物件数变化',
  `weight_delta`                 DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '实物重量变化 kg',
  `reserved_quantity_delta`      DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '预占件数变化',
  `reserved_weight_delta`        DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '预占重量变化 kg',
  `quantity_before`              DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '实物件数前余额',
  `quantity_after`               DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '实物件数后余额',
  `weight_before`                DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '实物重量前余额 kg',
  `weight_after`                 DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '实物重量后余额 kg',
  `reserved_quantity_before`     DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '预占件数前余额',
  `reserved_quantity_after`      DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '预占件数后余额',
  `reserved_weight_before`       DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '预占重量前余额 kg',
  `reserved_weight_after`        DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '预占重量后余额 kg',
  `available_quantity_before`    DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '可用件数前余额',
  `available_quantity_after`     DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '可用件数后余额',
  `available_weight_before`      DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '可用重量前余额 kg',
  `available_weight_after`       DECIMAL(14,3) NOT NULL DEFAULT 0.000 COMMENT '可用重量后余额 kg',
  `reason`                       VARCHAR(500)  DEFAULT NULL COMMENT '原因（报废/调整必填）',
  `operator_uuid`                VARCHAR(36)   DEFAULT NULL COMMENT '操作者 UUID，系统任务可为空',
  `operator_name`                VARCHAR(100)  NOT NULL COMMENT '操作者名称',
  `occurred_at`                  DATETIME(6)   NOT NULL COMMENT '业务发生时间',
  `idempotency_key`              VARCHAR(191)  NOT NULL COMMENT '业务命令幂等键',
  `payload_hash`                 CHAR(64)      NOT NULL COMMENT '命令内容 SHA-256 摘要',
  `created_at`                   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '写入时间',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_inventory_transaction_sequence` (`sequence_no`),
  UNIQUE KEY `uk_inventory_transaction_idempotency` (`idempotency_key`),
  KEY `idx_inventory_transaction_finish_time` (`finish_roll_uuid`, `occurred_at`, `created_at`, `uuid`),
  KEY `idx_inventory_transaction_source` (`source_business_type`, `source_business_uuid`),
  CONSTRAINT `fk_inventory_transaction_finish`
    FOREIGN KEY (`finish_roll_uuid`) REFERENCES `biz_finish_roll` (`uuid`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_inventory_transaction_event` CHECK (`event_type` IN
    ('OPENING_BALANCE','RECEIPT','RESERVE','RELEASE','ISSUE','RETURN','SCRAP','ADJUSTMENT')),
  CONSTRAINT `chk_inventory_transaction_non_negative` CHECK (
    `quantity_before` >= 0 AND `quantity_after` >= 0 AND
    `weight_before` >= 0 AND `weight_after` >= 0 AND
    `reserved_quantity_before` >= 0 AND `reserved_quantity_after` >= 0 AND
    `reserved_weight_before` >= 0 AND `reserved_weight_after` >= 0 AND
    `available_quantity_before` >= 0 AND `available_quantity_after` >= 0 AND
    `available_weight_before` >= 0 AND `available_weight_after` >= 0),
  CONSTRAINT `chk_inventory_transaction_balance_equation` CHECK (
    `quantity_after` = `quantity_before` + `quantity_delta` AND
    `weight_after` = `weight_before` + `weight_delta` AND
    `reserved_quantity_after` = `reserved_quantity_before` + `reserved_quantity_delta` AND
    `reserved_weight_after` = `reserved_weight_before` + `reserved_weight_delta` AND
    `available_quantity_before` = `quantity_before` - `reserved_quantity_before` AND
    `available_quantity_after` = `quantity_after` - `reserved_quantity_after`),
  CONSTRAINT `chk_inventory_transaction_event_delta` CHECK (
    (`event_type` = 'OPENING_BALANCE'
      AND `quantity_delta` >= 0 AND `weight_delta` >= 0
      AND `reserved_quantity_delta` >= 0 AND `reserved_weight_delta` >= 0
      AND `reserved_quantity_delta` <= `quantity_delta`
      AND `reserved_weight_delta` <= `weight_delta`)
    OR (`event_type` = 'RECEIPT'
      AND `quantity_delta` >= 0 AND `weight_delta` > 0
      AND `reserved_quantity_delta` = 0 AND `reserved_weight_delta` = 0)
    OR (`event_type` = 'RETURN'
      AND `quantity_delta` >= 0 AND `weight_delta` > 0
      AND `reserved_quantity_delta` >= 0 AND `reserved_weight_delta` >= 0
      AND (`reserved_weight_delta` = 0 OR `reserved_weight_delta` = `weight_delta`))
    OR (`event_type` = 'RESERVE'
      AND `quantity_delta` = 0 AND `weight_delta` = 0
      AND `reserved_weight_delta` > 0 AND `reserved_quantity_delta` >= 0)
    OR (`event_type` = 'RELEASE'
      AND `quantity_delta` = 0 AND `weight_delta` = 0
      AND `reserved_weight_delta` < 0 AND `reserved_quantity_delta` <= 0)
    OR (`event_type` = 'ISSUE'
      AND `weight_delta` < 0 AND `quantity_delta` <= 0
      AND `reserved_weight_delta` < 0 AND `reserved_quantity_delta` <= 0
      AND `reserved_weight_delta` = `weight_delta`)
    OR (`event_type` = 'SCRAP'
      AND `weight_delta` < 0 AND `quantity_delta` <= 0
      AND `reserved_weight_delta` = 0 AND `reserved_quantity_delta` = 0)
    OR (`event_type` = 'ADJUSTMENT'
      AND (`quantity_delta` <> 0 OR `weight_delta` <> 0
        OR `reserved_quantity_delta` <> 0 OR `reserved_weight_delta` <> 0))),
  CONSTRAINT `chk_inventory_transaction_reason` CHECK (
    `event_type` NOT IN ('SCRAP','ADJUSTMENT')
    OR (`reason` IS NOT NULL AND CHAR_LENGTH(TRIM(`reason`)) > 0)),
  CONSTRAINT `chk_inventory_transaction_available_weight` CHECK (
    `available_weight_before` = `weight_before` - `reserved_weight_before` AND
    `available_weight_after` = `weight_after` - `reserved_weight_after`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变成品库存流水账本';

DROP TRIGGER IF EXISTS `trg_inventory_transaction_no_update`;
DROP TRIGGER IF EXISTS `trg_inventory_transaction_no_delete`;

DELIMITER $$
CREATE TRIGGER `trg_inventory_transaction_no_update`
BEFORE UPDATE ON `biz_inventory_transaction`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'inventory transaction is append-only';
END$$

CREATE TRIGGER `trg_inventory_transaction_no_delete`
BEFORE DELETE ON `biz_inventory_transaction`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'inventory transaction is append-only';
END$$
DELIMITER ;
