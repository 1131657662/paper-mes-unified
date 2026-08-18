-- V3.73: isolate AI conversations by project-memory generation.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_ai_session_generation', 10) INTO @ai_session_generation_lock;
SET @ai_session_generation_guard_sql = IF(
  @ai_session_generation_lock = 1,
  'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''V3.73 migration lock not acquired'''
);
PREPARE ai_session_generation_guard FROM @ai_session_generation_guard_sql;
EXECUTE ai_session_generation_guard;
DEALLOCATE PREPARE ai_session_generation_guard;

SET @has_conversation_generation := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_conversation'
    AND column_name = 'memory_generation'
);
SET @conversation_generation_sql = IF(
  @has_conversation_generation = 0,
  'ALTER TABLE `biz_process_ai_conversation` ADD COLUMN `memory_generation` INT NOT NULL DEFAULT 1 AFTER `project_memory_version`',
  'DO 0'
);
PREPARE conversation_generation_stmt FROM @conversation_generation_sql;
EXECUTE conversation_generation_stmt;
DEALLOCATE PREPARE conversation_generation_stmt;

SET @has_message_generation := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_message'
    AND column_name = 'memory_generation'
);
SET @message_generation_sql = IF(
  @has_message_generation = 0,
  'ALTER TABLE `biz_process_ai_message` ADD COLUMN `memory_generation` INT NOT NULL DEFAULT 1 AFTER `conversation_id`',
  'DO 0'
);
PREPARE message_generation_stmt FROM @message_generation_sql;
EXECUTE message_generation_stmt;
DEALLOCATE PREPARE message_generation_stmt;

SET @has_parse_generation := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'memory_generation'
);
SET @parse_generation_sql = IF(
  @has_parse_generation = 0,
  'ALTER TABLE `biz_process_ai_parse` ADD COLUMN `memory_generation` INT NOT NULL DEFAULT 1 AFTER `parse_revision`',
  'DO 0'
);
PREPARE parse_generation_stmt FROM @parse_generation_sql;
EXECUTE parse_generation_stmt;
DEALLOCATE PREPARE parse_generation_stmt;

CREATE TABLE IF NOT EXISTS `biz_project_memory_learning_outbox` (
  `uuid` VARCHAR(36) NOT NULL,
  `event_key` VARCHAR(160) NOT NULL,
  `event_type` VARCHAR(32) NOT NULL,
  `payload_json` JSON NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  `attempt_count` INT NOT NULL DEFAULT 0,
  `next_attempt_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_error` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_memory_learning_event` (`event_key`),
  KEY `idx_memory_learning_due` (`status`, `next_attempt_at`),
  CONSTRAINT `chk_memory_learning_type`
    CHECK (`event_type` IN ('CONFIRMED_PARSE', 'SUBMITTED_ORDER')),
  CONSTRAINT `chk_memory_learning_status`
    CHECK (`status` IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Durable project-memory learning capture outbox';

SELECT RELEASE_LOCK('paper_mes_ai_session_generation') INTO @ai_session_generation_unlock;
