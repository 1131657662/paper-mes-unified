-- V3.69: order-scoped AI process parsing workflow.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_ai_process_workflow', 10) INTO @ai_process_workflow_lock;
SET @ai_process_workflow_guard_sql = IF(
  @ai_process_workflow_lock = 1,
  'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''V3.69 migration lock not acquired'''
);
PREPARE ai_process_workflow_guard FROM @ai_process_workflow_guard_sql;
EXECUTE ai_process_workflow_guard;
DEALLOCATE PREPARE ai_process_workflow_guard;

SET @has_order_ai_requirement := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_order'
    AND column_name = 'ai_requirement_json'
);
SET @ai_requirement_sql = IF(
  @has_order_ai_requirement = 0,
  'ALTER TABLE `biz_process_order` ADD COLUMN `ai_requirement_json` JSON DEFAULT NULL COMMENT ''AI确认后的订单级加工要求总览''',
  'SELECT 1'
);
PREPARE ai_requirement_stmt FROM @ai_requirement_sql;
EXECUTE ai_requirement_stmt;
DEALLOCATE PREPARE ai_requirement_stmt;

SET @has_draft_ai_intent := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_config_draft'
    AND column_name = 'ai_intent_json'
);
SET @ai_intent_sql = IF(
  @has_draft_ai_intent = 0,
  'ALTER TABLE `biz_process_config_draft` ADD COLUMN `ai_intent_json` JSON DEFAULT NULL COMMENT ''按母卷保存的AI工艺意图，不作为工艺计划解析''',
  'SELECT 1'
);
PREPARE ai_intent_stmt FROM @ai_intent_sql;
EXECUTE ai_intent_stmt;
DEALLOCATE PREPARE ai_intent_stmt;

INSERT IGNORE INTO `sys_process_catalog_billing_mode`
  (`catalog_uuid`, `billing_mode`, `sort_no`)
VALUES ('process-catalog-repack', 2, 20);

CREATE TABLE IF NOT EXISTS `biz_process_ai_conversation` (
  `uuid` VARCHAR(36) NOT NULL,
  `conversation_id` VARCHAR(64) NOT NULL,
  `order_uuid` VARCHAR(36) NOT NULL,
  `user_uuid` VARCHAR(36) NOT NULL,
  `current_step` TINYINT NOT NULL,
  `draft_version` INT NOT NULL,
  `project_memory_version` VARCHAR(32) NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'OPEN',
  `last_parse_revision` INT NOT NULL DEFAULT 0,
  `expires_at` DATETIME DEFAULT NULL,
  `closed_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_ai_conversation_id` (`conversation_id`),
  UNIQUE KEY `uk_ai_conversation_order` (`order_uuid`),
  KEY `idx_ai_conversation_status` (`status`, `updated_at`),
  CONSTRAINT `fk_ai_conversation_order` FOREIGN KEY (`order_uuid`)
    REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_ai_conversation_step` CHECK (`current_step` IN (3, 4)),
  CONSTRAINT `chk_ai_conversation_status`
    CHECK (`status` IN ('OPEN', 'INTERRUPTED', 'CLOSED', 'EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单级AI工艺解析会话';

CREATE TABLE IF NOT EXISTS `biz_process_ai_message` (
  `uuid` VARCHAR(36) NOT NULL,
  `conversation_id` VARCHAR(64) NOT NULL,
  `sequence_no` INT NOT NULL,
  `role` VARCHAR(16) NOT NULL,
  `message_status` VARCHAR(16) NOT NULL DEFAULT 'FINAL',
  `idempotency_key` VARCHAR(80) NOT NULL,
  `content_ciphertext` MEDIUMTEXT NOT NULL COMMENT 'AES-GCM密文，不写明文日志',
  `content_hash` CHAR(64) NOT NULL,
  `structured_result` JSON DEFAULT NULL COMMENT 'AES-GCM envelope; legacy rows may contain sanitized JSON',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_ai_message_sequence` (`conversation_id`, `sequence_no`),
  UNIQUE KEY `uk_ai_message_idempotency` (`conversation_id`, `idempotency_key`),
  KEY `idx_ai_message_created` (`conversation_id`, `created_at`),
  CONSTRAINT `fk_ai_message_conversation` FOREIGN KEY (`conversation_id`)
    REFERENCES `biz_process_ai_conversation` (`conversation_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_ai_message_role` CHECK (`role` IN ('USER', 'ASSISTANT', 'SYSTEM')),
  CONSTRAINT `chk_ai_message_status` CHECK (`message_status` IN ('PARTIAL', 'FINAL', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单级AI会话消息';

CREATE TABLE IF NOT EXISTS `biz_process_ai_parse` (
  `uuid` VARCHAR(36) NOT NULL,
  `order_uuid` VARCHAR(36) NOT NULL,
  `conversation_id` VARCHAR(64) NOT NULL,
  `parse_id` VARCHAR(64) NOT NULL,
  `parse_revision` INT NOT NULL,
  `request_idempotency_key` VARCHAR(80) NOT NULL,
  `apply_idempotency_key` VARCHAR(80) DEFAULT NULL,
  `expected_version` INT NOT NULL,
  `status` VARCHAR(24) NOT NULL,
  `provider` VARCHAR(32) NOT NULL,
  `model` VARCHAR(80) NOT NULL,
  `model_version` VARCHAR(80) DEFAULT NULL,
  `route` VARCHAR(32) NOT NULL,
  `schema_version` VARCHAR(16) NOT NULL,
  `project_memory_version` VARCHAR(32) NOT NULL,
  `project_memory_checksum` CHAR(71) NOT NULL,
  `project_memory_item_ids` JSON NOT NULL,
  `intent_json` JSON NOT NULL COMMENT '脱敏结构化意图，不含客户原文',
  `accepted_field_paths` JSON DEFAULT NULL COMMENT '用户明确确认应用的字段路径',
  `result_hash` CHAR(64) NOT NULL,
  `plan_hash` CHAR(64) DEFAULT NULL,
  `next_version` INT DEFAULT NULL,
  `confirmed_result_json` JSON DEFAULT NULL COMMENT 'AES-GCM envelope for idempotent replay',
  `confirmed_by` VARCHAR(64) DEFAULT NULL,
  `confirmed_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_ai_parse_id` (`parse_id`),
  UNIQUE KEY `uk_ai_parse_conversation_revision` (`conversation_id`, `parse_revision`),
  UNIQUE KEY `uk_ai_parse_request_idempotency` (`conversation_id`, `request_idempotency_key`),
  UNIQUE KEY `uk_ai_parse_apply_idempotency` (`parse_id`, `apply_idempotency_key`),
  KEY `idx_ai_parse_order_version` (`order_uuid`, `expected_version`),
  KEY `idx_ai_parse_conversation_status` (`conversation_id`, `status`, `created_at`),
  CONSTRAINT `fk_ai_parse_order` FOREIGN KEY (`order_uuid`)
    REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_parse_conversation` FOREIGN KEY (`conversation_id`)
    REFERENCES `biz_process_ai_conversation` (`conversation_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_ai_parse_status`
    CHECK (`status` IN ('READY', 'CLARIFICATION', 'INTERRUPTED', 'CONFIRMED', 'EXPIRED', 'REJECTED')),
  CONSTRAINT `chk_ai_parse_memory_checksum`
    CHECK (`project_memory_checksum` LIKE 'sha256:%' AND CHAR_LENGTH(`project_memory_checksum`) = 71),
  CONSTRAINT `chk_ai_parse_confirmation`
    CHECK (`status` <> 'CONFIRMED' OR (`apply_idempotency_key` IS NOT NULL
      AND `accepted_field_paths` IS NOT NULL AND `plan_hash` IS NOT NULL
      AND `next_version` = `expected_version` + 1 AND `confirmed_result_json` IS NOT NULL
      AND `confirmed_by` IS NOT NULL AND `confirmed_at` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI工艺解析不可变候选与确认记录';

CREATE TABLE IF NOT EXISTS `sys_ai_call_audit` (
  `uuid` VARCHAR(36) NOT NULL,
  `order_uuid` VARCHAR(36) NOT NULL,
  `conversation_id` VARCHAR(64) DEFAULT NULL,
  `parse_id` VARCHAR(64) DEFAULT NULL,
  `expected_version` INT DEFAULT NULL,
  `action` VARCHAR(16) NOT NULL,
  `idempotency_key` VARCHAR(80) DEFAULT NULL,
  `schema_version` VARCHAR(16) DEFAULT NULL,
  `project_memory_version` VARCHAR(32) DEFAULT NULL,
  `project_memory_checksum` CHAR(71) DEFAULT NULL,
  `project_memory_item_ids` JSON DEFAULT NULL,
  `request_hash` CHAR(64) NOT NULL,
  `result_hash` CHAR(64) DEFAULT NULL,
  `provider` VARCHAR(32) NOT NULL,
  `model` VARCHAR(80) NOT NULL,
  `route` VARCHAR(32) NOT NULL,
  `outcome` VARCHAR(32) NOT NULL,
  `failure_code` VARCHAR(64) DEFAULT NULL,
  `latency_ms` INT DEFAULT NULL,
  `input_tokens` INT DEFAULT NULL,
  `output_tokens` INT DEFAULT NULL,
  `created_by` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  KEY `idx_ai_audit_order_time` (`order_uuid`, `created_at`),
  UNIQUE KEY `uk_ai_audit_request` (`request_hash`, `action`),
  UNIQUE KEY `uk_ai_audit_idempotency` (`order_uuid`, `idempotency_key`, `action`),
  KEY `idx_ai_audit_parse` (`parse_id`),
  CONSTRAINT `chk_ai_audit_action` CHECK (`action` IN ('START', 'CLARIFY', 'CONFIRM')),
  CONSTRAINT `chk_ai_audit_checksum`
    CHECK (`project_memory_checksum` IS NULL OR (`project_memory_checksum` LIKE 'sha256:%' AND CHAR_LENGTH(`project_memory_checksum`) = 71))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI工艺解析调用审计';

CREATE TABLE IF NOT EXISTS `sys_ai_provider_secret` (
  `provider` VARCHAR(32) NOT NULL,
  `api_key_ciphertext` TEXT NOT NULL COMMENT 'AES-GCM ciphertext; plaintext is never stored',
  `api_key_last_four` VARCHAR(8) NOT NULL,
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `updated_by` VARCHAR(64) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`provider`),
  CONSTRAINT `chk_ai_provider_secret_provider` CHECK (`provider` IN ('DEEPSEEK', 'ZHIPU')),
  CONSTRAINT `chk_ai_provider_secret_enabled` CHECK (`enabled` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Encrypted AI provider credentials';

SELECT RELEASE_LOCK('paper_mes_ai_process_workflow') INTO @ai_process_workflow_unlock;
