-- V3.68: audit and idempotency records for project-memory mutations.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_ai_project_memory_audit', 10) INTO @ai_project_memory_audit_lock;
SET @ai_project_memory_audit_guard_sql = IF(
  @ai_project_memory_audit_lock = 1,
  'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''V3.68 migration lock not acquired'''
);
PREPARE ai_project_memory_audit_guard FROM @ai_project_memory_audit_guard_sql;
EXECUTE ai_project_memory_audit_guard;
DEALLOCATE PREPARE ai_project_memory_audit_guard;

CREATE TABLE IF NOT EXISTS `biz_project_memory_patch_audit` (
  `uuid` VARCHAR(36) NOT NULL,
  `idempotency_key` VARCHAR(128) NOT NULL,
  `operation_type` VARCHAR(16) NOT NULL,
  `expected_memory_version` VARCHAR(32) DEFAULT NULL,
  `old_doc_version` VARCHAR(32) DEFAULT NULL,
  `new_doc_version` VARCHAR(32) DEFAULT NULL,
  `old_checksum` CHAR(71) DEFAULT NULL,
  `new_checksum` CHAR(71) DEFAULT NULL,
  `operations_json` JSON NOT NULL,
  `reason` VARCHAR(500) NOT NULL,
  `operator` VARCHAR(64) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_project_memory_patch_idempotency` (`idempotency_key`),
  KEY `idx_project_memory_patch_created` (`created_at`),
  CONSTRAINT `chk_project_memory_patch_operation`
    CHECK (`operation_type` IN ('PATCH', 'ROLLBACK', 'RELOAD'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI项目记忆变更审计';

SELECT RELEASE_LOCK('paper_mes_ai_project_memory_audit') INTO @ai_project_memory_audit_unlock;
