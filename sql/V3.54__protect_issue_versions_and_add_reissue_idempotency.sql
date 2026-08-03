-- V3.54: protect terminal issue versions and make reissue preparation idempotent.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_process_order_issue_versions', 10) INTO @issue_version_lock;
SET @issue_version_lock_guard_sql = IF(
  @issue_version_lock = 1,
  'DO 0',
  'SELECT V3_54_MIGRATION_LOCK_NOT_ACQUIRED'
);
PREPARE issue_version_lock_guard FROM @issue_version_lock_guard_sql;
EXECUTE issue_version_lock_guard;
DEALLOCATE PREPARE issue_version_lock_guard;

SET @request_id_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_order_issue_version'
    AND column_name = 'request_id'
);
SET @add_request_id_sql := IF(
  @request_id_column_exists = 0,
  'ALTER TABLE `biz_process_order_issue_version` ADD COLUMN `request_id` VARCHAR(64) DEFAULT NULL COMMENT ''reissue request idempotency key'' AFTER `issue_operator_name`',
  'SELECT 1'
);
PREPARE add_request_id FROM @add_request_id_sql;
EXECUTE add_request_id;
DEALLOCATE PREPARE add_request_id;

SET @payload_hash_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_order_issue_version'
    AND column_name = 'payload_hash'
);
SET @add_payload_hash_sql := IF(
  @payload_hash_column_exists = 0,
  'ALTER TABLE `biz_process_order_issue_version` ADD COLUMN `payload_hash` CHAR(64) DEFAULT NULL COMMENT ''reissue payload SHA-256 digest'' AFTER `request_id`',
  'SELECT 1'
);
PREPARE add_payload_hash FROM @add_payload_hash_sql;
EXECUTE add_payload_hash;
DEALLOCATE PREPARE add_payload_hash;

SET @request_id_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_order_issue_version'
    AND index_name = 'uk_process_order_issue_request'
);
SET @add_request_id_index_sql := IF(
  @request_id_index_exists = 0,
  'ALTER TABLE `biz_process_order_issue_version` ADD UNIQUE KEY `uk_process_order_issue_request` (`order_uuid`, `request_id`)',
  'SELECT 1'
);
PREPARE add_request_id_index FROM @add_request_id_index_sql;
EXECUTE add_request_id_index;
DEALLOCATE PREPARE add_request_id_index;

DROP TRIGGER IF EXISTS `trg_process_order_issue_version_no_terminal_update`;
DROP TRIGGER IF EXISTS `trg_process_order_issue_version_no_terminal_delete`;

DELIMITER $$
CREATE TRIGGER `trg_process_order_issue_version_no_terminal_update`
BEFORE UPDATE ON `biz_process_order_issue_version`
FOR EACH ROW
BEGIN
  IF OLD.`status` IN ('APPLIED', 'ARCHIVED') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'terminal process order issue version is immutable';
  END IF;
END$$

CREATE TRIGGER `trg_process_order_issue_version_no_terminal_delete`
BEFORE DELETE ON `biz_process_order_issue_version`
FOR EACH ROW
BEGIN
  IF OLD.`status` IN ('APPLIED', 'ARCHIVED') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'terminal process order issue version cannot be deleted';
  END IF;
END$$
DELIMITER ;

SELECT RELEASE_LOCK('paper_mes_process_order_issue_versions') INTO @issue_version_unlock;
