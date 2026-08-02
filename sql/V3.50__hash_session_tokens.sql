-- V3.50: replace persisted session tokens with one-way SHA-256 digests.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @token_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user_session'
    AND column_name = 'token'
);
SET @sql := IF(
  @token_column_exists = 0,
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''sys_user_session.token is missing''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `sys_user_session`
SET `token` = LOWER(SHA2(`token`, 256))
WHERE `token` IS NOT NULL
  AND (`token` NOT REGEXP '^[0-9a-fA-F]{64}$' OR CHAR_LENGTH(`token`) <> 64);

ALTER TABLE `sys_user_session`
  MODIFY COLUMN `token` VARCHAR(64) NOT NULL COMMENT 'SHA-256 session token digest';
