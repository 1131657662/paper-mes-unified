-- V3.66: auditable post-issue source-roll disposition.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_process_roll_disposition', 10) INTO @process_roll_disposition_lock;
SET @process_roll_disposition_guard_sql = IF(
  @process_roll_disposition_lock = 1,
  'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''V3.66 migration lock not acquired'''
);
PREPARE process_roll_disposition_guard FROM @process_roll_disposition_guard_sql;
EXECUTE process_roll_disposition_guard;
DEALLOCATE PREPARE process_roll_disposition_guard;

SET @has_disposition_action := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_original_roll'
    AND column_name = 'disposition_action');
SET @sql := IF(@has_disposition_action = 0,
  'ALTER TABLE biz_original_roll ADD COLUMN disposition_action VARCHAR(32) DEFAULT NULL COMMENT ''下发后处置动作，与报废状态分离'' AFTER roll_status',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_disposition_index := (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_original_roll'
    AND index_name = 'idx_original_roll_disposition');
SET @sql := IF(@has_disposition_index = 0,
  'ALTER TABLE biz_original_roll ADD KEY idx_original_roll_disposition (order_uuid, disposition_action)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `biz_process_roll_disposition` (
  `uuid` VARCHAR(36) NOT NULL,
  `source_order_uuid` VARCHAR(36) NOT NULL,
  `source_roll_uuid` VARCHAR(36) NOT NULL,
  `action_type` VARCHAR(32) NOT NULL,
  `status` VARCHAR(16) NOT NULL,
  `target_order_uuid` VARCHAR(36) DEFAULT NULL,
  `target_roll_uuid` VARCHAR(36) DEFAULT NULL,
  `target_finish_uuid` VARCHAR(36) DEFAULT NULL,
  `target_finish_uuids` JSON DEFAULT NULL COMMENT '直发生成的全部成品UUID数组',
  `request_id` VARCHAR(64) NOT NULL,
  `reason` VARCHAR(500) NOT NULL,
  `operator` VARCHAR(128) NOT NULL,
  `operate_time` DATETIME NOT NULL,
  `source_order_version` INT DEFAULT NULL,
  `source_roll_version` INT DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(128) DEFAULT NULL,
  `update_by` VARCHAR(128) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `version` INT NOT NULL DEFAULT 0,
  `ext_str1` VARCHAR(255) DEFAULT NULL,
  `ext_str2` VARCHAR(255) DEFAULT NULL,
  `ext_num1` DECIMAL(20,6) DEFAULT NULL,
  `ext_num2` DECIMAL(20,6) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_process_roll_disposition_source` (`source_roll_uuid`, `is_deleted`),
  UNIQUE KEY `uk_process_roll_disposition_request` (`request_id`, `is_deleted`),
  KEY `idx_process_roll_disposition_order` (`source_order_uuid`, `operate_time`),
  CONSTRAINT `fk_process_roll_disposition_order` FOREIGN KEY (`source_order_uuid`)
    REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_process_roll_disposition_roll` FOREIGN KEY (`source_roll_uuid`)
    REFERENCES `biz_original_roll` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_process_roll_disposition_target_order` FOREIGN KEY (`target_order_uuid`)
    REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_process_roll_disposition_target_roll` FOREIGN KEY (`target_roll_uuid`)
    REFERENCES `biz_original_roll` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_process_roll_disposition_target_finish` FOREIGN KEY (`target_finish_uuid`)
    REFERENCES `biz_finish_roll` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_process_roll_disposition_action`
    CHECK (`action_type` IN ('DIRECT_SHIP', 'CANCEL', 'SPLIT_TO_ORDER')),
  CONSTRAINT `chk_process_roll_disposition_status`
    CHECK (`status` IN ('APPLIED', 'REJECTED')),
  CONSTRAINT `chk_process_roll_disposition_reason`
    CHECK (CHAR_LENGTH(TRIM(`reason`)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='涓嬪彂鍚庢湭鍔犲伐姣嶅嵎澶勭疆瀹¤';

SET @has_target_finish_uuids := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_roll_disposition'
    AND column_name = 'target_finish_uuids');
SET @sql := IF(@has_target_finish_uuids = 0,
  'ALTER TABLE biz_process_roll_disposition ADD COLUMN target_finish_uuids JSON DEFAULT NULL COMMENT ''直发生成的全部成品UUID数组'' AFTER target_finish_uuid',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT RELEASE_LOCK('paper_mes_process_roll_disposition') INTO @process_roll_disposition_unlock;
