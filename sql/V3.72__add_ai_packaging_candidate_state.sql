-- V3.72: durable lifecycle for confirmed AI packaging candidates.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_ai_packaging_candidate_state', 10) INTO @ai_packaging_state_lock;
SET @ai_packaging_state_guard_sql = IF(
  @ai_packaging_state_lock = 1,
  'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''V3.72 migration lock not acquired'''
);
PREPARE ai_packaging_state_guard FROM @ai_packaging_state_guard_sql;
EXECUTE ai_packaging_state_guard;
DEALLOCATE PREPARE ai_packaging_state_guard;

CREATE TABLE IF NOT EXISTS `biz_process_ai_packaging_candidate` (
  `uuid` VARCHAR(36) NOT NULL,
  `order_uuid` VARCHAR(36) NOT NULL,
  `conversation_id` VARCHAR(64) NOT NULL,
  `parse_id` VARCHAR(64) NOT NULL,
  `parse_revision` INT NOT NULL,
  `owner_roll_ref` VARCHAR(32) NOT NULL,
  `original_uuid` VARCHAR(36) NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  `created_by` VARCHAR(64) NOT NULL,
  `resolved_by` VARCHAR(64) DEFAULT NULL,
  `resolved_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_ai_packaging_candidate_parse_owner` (`parse_id`, `owner_roll_ref`),
  KEY `idx_ai_packaging_candidate_pending` (`order_uuid`, `created_by`, `status`, `created_at`),
  KEY `idx_ai_packaging_candidate_conversation` (`conversation_id`, `status`),
  CONSTRAINT `fk_ai_packaging_candidate_order` FOREIGN KEY (`order_uuid`)
    REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_packaging_candidate_conversation` FOREIGN KEY (`conversation_id`)
    REFERENCES `biz_process_ai_conversation` (`conversation_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_packaging_candidate_parse` FOREIGN KEY (`parse_id`)
    REFERENCES `biz_process_ai_parse` (`parse_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_ai_packaging_candidate_status`
    CHECK (`status` IN ('PENDING', 'SAVED', 'DISMISSED')),
  CONSTRAINT `chk_ai_packaging_candidate_resolution`
    CHECK (`status` = 'PENDING' OR (`resolved_by` IS NOT NULL AND `resolved_at` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI包装候选待确认状态';

SELECT RELEASE_LOCK('paper_mes_ai_packaging_candidate_state') INTO @ai_packaging_state_unlock;
