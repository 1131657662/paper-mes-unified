-- V3.67: database-backed project memory snapshots.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_ai_project_memory', 10) INTO @ai_project_memory_lock;
SET @ai_project_memory_guard_sql = IF(
  @ai_project_memory_lock = 1,
  'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''V3.67 migration lock not acquired'''
);
PREPARE ai_project_memory_guard FROM @ai_project_memory_guard_sql;
EXECUTE ai_project_memory_guard;
DEALLOCATE PREPARE ai_project_memory_guard;

CREATE TABLE IF NOT EXISTS `biz_project_memory_doc` (
  `uuid` VARCHAR(36) NOT NULL,
  `doc_version` VARCHAR(32) NOT NULL,
  `schema_version` VARCHAR(16) NOT NULL,
  `checksum` CHAR(71) NOT NULL,
  `doc_json` JSON NOT NULL,
  `status` VARCHAR(16) NOT NULL,
  `patch_notes` VARCHAR(500) DEFAULT NULL,
  `created_by` VARCHAR(64) NOT NULL,
  `approved_by` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `active_status` VARCHAR(16) GENERATED ALWAYS AS (
    CASE WHEN `status` = 'ACTIVE' THEN 'ACTIVE' ELSE NULL END
  ) STORED,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_project_memory_doc_version` (`doc_version`),
  UNIQUE KEY `uk_project_memory_doc_checksum` (`checksum`),
  UNIQUE KEY `uk_project_memory_active_status` (`active_status`),
  KEY `idx_project_memory_doc_status` (`status`, `created_at`),
  CONSTRAINT `chk_project_memory_doc_status`
    CHECK (`status` IN ('ACTIVE', 'SUPERSEDED', 'DRAFT')),
  CONSTRAINT `chk_project_memory_doc_checksum`
    CHECK (`checksum` LIKE 'sha256:%' AND CHAR_LENGTH(`checksum`) = 71)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI项目记忆全量版本快照';

SELECT RELEASE_LOCK('paper_mes_ai_project_memory') INTO @ai_project_memory_unlock;
