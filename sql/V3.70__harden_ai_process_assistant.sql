-- V3.70: durable AI draft application, adaptive-memory evidence, and append-only audit.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_ai_process_hardening', 10) INTO @ai_hardening_lock;
SET @ai_hardening_guard_sql = IF(
  @ai_hardening_lock = 1,
  'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''V3.70 migration lock not acquired'''
);
PREPARE ai_hardening_guard FROM @ai_hardening_guard_sql;
EXECUTE ai_hardening_guard;
DEALLOCATE PREPARE ai_hardening_guard;

CREATE TABLE IF NOT EXISTS `biz_project_memory_candidate` (
  `uuid` VARCHAR(36) NOT NULL,
  `memory_id` VARCHAR(96) NOT NULL,
  `candidate_type` VARCHAR(24) NOT NULL,
  `candidate_json` JSON NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'CANDIDATE',
  `distinct_order_count` INT NOT NULL DEFAULT 0,
  `first_seen_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_seen_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` DATETIME NOT NULL,
  `reviewed_by` VARCHAR(64) DEFAULT NULL,
  `review_notes` VARCHAR(500) DEFAULT NULL,
  `reviewed_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_memory_candidate_id` (`memory_id`),
  KEY `idx_memory_candidate_status` (`status`, `last_seen_at`),
  CONSTRAINT `chk_memory_candidate_type`
    CHECK (`candidate_type` IN ('TERM', 'EXAMPLE', 'RULE', 'EXTERNAL_FACT', 'EPISODE')),
  CONSTRAINT `chk_memory_candidate_status`
    CHECK (`status` IN ('CANDIDATE', 'READY', 'ACTIVE', 'CONFLICT', 'REJECTED', 'EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Controlled adaptive project-memory candidates';

CREATE TABLE IF NOT EXISTS `biz_project_memory_candidate_evidence` (
  `uuid` VARCHAR(36) NOT NULL,
  `candidate_uuid` VARCHAR(36) NOT NULL,
  `order_uuid` VARCHAR(36) NOT NULL,
  `parse_id` VARCHAR(64) NOT NULL,
  `evidence_hash` CHAR(64) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_memory_candidate_order` (`candidate_uuid`, `order_uuid`),
  KEY `idx_memory_evidence_parse` (`parse_id`),
  CONSTRAINT `fk_memory_evidence_candidate` FOREIGN KEY (`candidate_uuid`)
    REFERENCES `biz_project_memory_candidate` (`uuid`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_memory_evidence_order` FOREIGN KEY (`order_uuid`)
    REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_memory_evidence_parse` FOREIGN KEY (`parse_id`)
    REFERENCES `biz_process_ai_parse` (`parse_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Distinct-order evidence for memory promotion';

SET @has_audit_request_key := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sys_ai_call_audit'
    AND index_name = 'uk_ai_audit_request'
);
SET @drop_audit_request_sql = IF(
  @has_audit_request_key > 0,
  'ALTER TABLE `sys_ai_call_audit` DROP INDEX `uk_ai_audit_request`',
  'SELECT 1'
);
PREPARE drop_audit_request_stmt FROM @drop_audit_request_sql;
EXECUTE drop_audit_request_stmt;
DEALLOCATE PREPARE drop_audit_request_stmt;

SET @has_audit_idempotency_key := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sys_ai_call_audit'
    AND index_name = 'uk_ai_audit_idempotency'
);
SET @drop_audit_idempotency_sql = IF(
  @has_audit_idempotency_key > 0,
  'ALTER TABLE `sys_ai_call_audit` DROP INDEX `uk_ai_audit_idempotency`',
  'SELECT 1'
);
PREPARE drop_audit_idempotency_stmt FROM @drop_audit_idempotency_sql;
EXECUTE drop_audit_idempotency_stmt;
DEALLOCATE PREPARE drop_audit_idempotency_stmt;

SET @has_audit_attempt_index := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sys_ai_call_audit'
    AND index_name = 'idx_ai_audit_attempt'
);
SET @add_audit_attempt_sql = IF(
  @has_audit_attempt_index = 0,
  'ALTER TABLE `sys_ai_call_audit` ADD KEY `idx_ai_audit_attempt` (`order_uuid`, `idempotency_key`, `action`, `created_at`)',
  'SELECT 1'
);
PREPARE add_audit_attempt_stmt FROM @add_audit_attempt_sql;
EXECUTE add_audit_attempt_stmt;
DEALLOCATE PREPARE add_audit_attempt_stmt;

SELECT RELEASE_LOCK('paper_mes_ai_process_hardening') INTO @ai_hardening_unlock;
