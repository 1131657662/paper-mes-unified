-- V3.61: persist append-roll sessions and allow at most one active session per process order.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_process_order_append_sessions', 10) INTO @append_session_lock;
SET @append_session_lock_sql := IF(
  @append_session_lock = 1,
  'DO 0',
  'SELECT V3_61_MIGRATION_LOCK_NOT_ACQUIRED'
);
PREPARE append_session_lock_guard FROM @append_session_lock_sql;
EXECUTE append_session_lock_guard;
DEALLOCATE PREPARE append_session_lock_guard;

CREATE TABLE IF NOT EXISTS `biz_process_order_append_session` (
  `uuid` VARCHAR(36) NOT NULL,
  `order_uuid` VARCHAR(36) NOT NULL,
  `session_no` VARCHAR(64) NOT NULL,
  `base_order_version` INT NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  `reason` VARCHAR(255) DEFAULT NULL,
  `operator` VARCHAR(100) DEFAULT NULL,
  `commit_request_id` VARCHAR(64) DEFAULT NULL,
  `apply_time` DATETIME DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `active_order_uuid` VARCHAR(36) GENERATED ALWAYS AS
    (CASE WHEN `is_deleted` = 0 AND `status` IN ('DRAFT','READY')
      THEN `order_uuid` ELSE NULL END) STORED,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_process_append_session_no` (`session_no`),
  UNIQUE KEY `uk_process_append_active_order` (`active_order_uuid`),
  KEY `idx_process_append_order_status` (`order_uuid`,`status`,`is_deleted`),
  CONSTRAINT `fk_process_append_session_order` FOREIGN KEY (`order_uuid`)
    REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_process_append_session_status` CHECK
    (`status` IN ('DRAFT','READY','APPLIED','CANCELLED','EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='process order append sessions';

CREATE TABLE IF NOT EXISTS `biz_process_order_append_roll` (
  `uuid` VARCHAR(36) NOT NULL,
  `session_uuid` VARCHAR(36) NOT NULL,
  `row_sort` INT NOT NULL,
  `extra_no` VARCHAR(100) DEFAULT NULL,
  `roll_no` VARCHAR(100) DEFAULT NULL,
  `paper_name` VARCHAR(100) NOT NULL,
  `gram_weight` INT NOT NULL,
  `original_width` INT NOT NULL,
  `original_diameter` INT DEFAULT NULL,
  `core_diameter` INT DEFAULT NULL,
  `original_length` INT DEFAULT NULL,
  `roll_weight` DECIMAL(12,3) NOT NULL,
  `piece_num` INT NOT NULL DEFAULT 1,
  `batch_no` VARCHAR(100) DEFAULT NULL,
  `damage_desc` VARCHAR(255) DEFAULT NULL,
  `process_mode` TINYINT DEFAULT NULL,
  `main_step_type` TINYINT DEFAULT NULL,
  `machine_uuid` VARCHAR(36) DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `config_json` JSON DEFAULT NULL,
  `preview_json` JSON DEFAULT NULL,
  `config_status` TINYINT NOT NULL DEFAULT 0,
  `config_type` VARCHAR(20) DEFAULT 'singlePlan',
  `service_steps_json` JSON DEFAULT NULL,
  `last_error` VARCHAR(500) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 1,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(12,3) DEFAULT NULL,
  `ext_num2` DECIMAL(12,3) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_process_append_roll_sort` (`session_uuid`,`row_sort`,`is_deleted`),
  KEY `idx_process_append_roll_session` (`session_uuid`,`is_deleted`),
  CONSTRAINT `fk_process_append_roll_session` FOREIGN KEY (`session_uuid`)
    REFERENCES `biz_process_order_append_session` (`uuid`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_process_append_roll_config_status` CHECK (`config_status` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='process order append roll drafts';

SET @append_session_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_process_order_append_session'
     AND column_name = 'commit_request_id') = 0,
  'ALTER TABLE `biz_process_order_append_session` ADD COLUMN `commit_request_id` VARCHAR(64) DEFAULT NULL AFTER `operator`',
  'DO 0'
);
PREPARE append_session_ddl FROM @append_session_ddl_sql;
EXECUTE append_session_ddl;
DEALLOCATE PREPARE append_session_ddl;

SET @append_session_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_process_order_append_roll'
     AND column_name = 'service_steps_json') = 0,
  'ALTER TABLE `biz_process_order_append_roll` ADD COLUMN `service_steps_json` JSON DEFAULT NULL AFTER `config_type`',
  'DO 0'
);
PREPARE append_session_ddl FROM @append_session_ddl_sql;
EXECUTE append_session_ddl;
DEALLOCATE PREPARE append_session_ddl;

SET @append_session_conflicts := (
  SELECT COUNT(*) FROM (
    SELECT `order_uuid`
    FROM `biz_process_order_append_session`
    WHERE `is_deleted` = 0 AND `status` IN ('DRAFT','READY')
    GROUP BY `order_uuid`
    HAVING COUNT(*) > 1
  ) duplicate_active_sessions
);
SET @append_session_guard_sql := IF(
  @append_session_conflicts = 0,
  'DO 0',
  'SELECT V3_61_DUPLICATE_ACTIVE_APPEND_SESSIONS'
);
PREPARE append_session_guard FROM @append_session_guard_sql;
EXECUTE append_session_guard;
DEALLOCATE PREPARE append_session_guard;

SET @append_session_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_process_order_append_session'
     AND column_name = 'active_order_uuid') = 0,
  'ALTER TABLE `biz_process_order_append_session` ADD COLUMN `active_order_uuid` VARCHAR(36) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 AND `status` IN (''DRAFT'',''READY'') THEN `order_uuid` ELSE NULL END) STORED AFTER `is_deleted`',
  'DO 0'
);
PREPARE append_session_ddl FROM @append_session_ddl_sql;
EXECUTE append_session_ddl;
DEALLOCATE PREPARE append_session_ddl;

SET @append_session_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_process_order_append_session'
     AND index_name = 'uk_process_append_active_order') = 0,
  'ALTER TABLE `biz_process_order_append_session` ADD UNIQUE KEY `uk_process_append_active_order` (`active_order_uuid`)',
  'DO 0'
);
PREPARE append_session_ddl FROM @append_session_ddl_sql;
EXECUTE append_session_ddl;
DEALLOCATE PREPARE append_session_ddl;

SELECT RELEASE_LOCK('paper_mes_process_order_append_sessions') INTO @append_session_unlock;
