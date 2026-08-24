-- V3.76: idempotency keys are isolated with the conversation memory generation.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_ai_message_idempotency_generation', 10) INTO @ai_message_idempotency_lock;
SET @ai_message_idempotency_guard_sql = IF(
  @ai_message_idempotency_lock = 1,
  'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''V3.76 migration lock not acquired'''
);
PREPARE ai_message_idempotency_guard FROM @ai_message_idempotency_guard_sql;
EXECUTE ai_message_idempotency_guard;
DEALLOCATE PREPARE ai_message_idempotency_guard;

SET @has_message_table := (
  SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_message'
);
SET @message_table_guard_sql = IF(
  @has_message_table = 1,
  'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''biz_process_ai_message is missing'''
);
PREPARE message_table_guard FROM @message_table_guard_sql;
EXECUTE message_table_guard;
DEALLOCATE PREPARE message_table_guard;

SET @has_generation := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_message'
    AND column_name = 'memory_generation'
);
SET @generation_guard_sql = IF(
  @has_generation = 1,
  'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''memory_generation is missing'''
);
PREPARE generation_guard FROM @generation_guard_sql;
EXECUTE generation_guard;
DEALLOCATE PREPARE generation_guard;

SET @has_old_key := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_message'
    AND index_name = 'uk_ai_message_idempotency'
);
SET @drop_old_key_sql = IF(@has_old_key = 1,
  'ALTER TABLE biz_process_ai_message DROP INDEX uk_ai_message_idempotency', 'DO 0');
PREPARE drop_old_key FROM @drop_old_key_sql;
EXECUTE drop_old_key;
DEALLOCATE PREPARE drop_old_key;

SET @has_scoped_key := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_message'
    AND index_name = 'uk_ai_message_idempotency_generation'
);
SET @add_scoped_key_sql = IF(@has_scoped_key = 0,
  'ALTER TABLE biz_process_ai_message ADD UNIQUE KEY uk_ai_message_idempotency_generation (conversation_id, memory_generation, idempotency_key)',
  'DO 0');
PREPARE add_scoped_key FROM @add_scoped_key_sql;
EXECUTE add_scoped_key;
DEALLOCATE PREPARE add_scoped_key;

SELECT RELEASE_LOCK('paper_mes_ai_message_idempotency_generation') INTO @ai_message_idempotency_unlock;
