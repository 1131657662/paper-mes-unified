-- Separate post-production operational commentary from the immutable issued production remark.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_process_order_post_production_note', 10) INTO @post_note_lock;
SET @post_note_guard_sql = IF(
  @post_note_lock = 1,
  'DO 0',
  'SELECT V3_64_MIGRATION_LOCK_NOT_ACQUIRED'
);
PREPARE post_note_guard FROM @post_note_guard_sql;
EXECUTE post_note_guard;
DEALLOCATE PREPARE post_note_guard;

SET @post_note_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_order'
    AND column_name = 'post_production_note'
);
SET @add_post_note_sql := IF(
  @post_note_column_exists = 0,
  'ALTER TABLE `biz_process_order` ADD COLUMN `post_production_note` TEXT DEFAULT NULL COMMENT ''Post-production operational note; excluded from issued snapshots'' AFTER `remark_long`',
  'SELECT 1'
);
PREPARE add_post_note FROM @add_post_note_sql;
EXECUTE add_post_note;
DEALLOCATE PREPARE add_post_note;

SELECT RELEASE_LOCK('paper_mes_process_order_post_production_note') INTO @post_note_unlock;
