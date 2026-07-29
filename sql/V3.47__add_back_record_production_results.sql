-- V3.47: preserve planned finish rows while recording actual output count during back-recording.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_back_record_production_result', 10) INTO @back_record_production_lock;
SET @back_record_production_lock_guard_sql = IF(
  @back_record_production_lock = 1,
  'DO 0',
  'SELECT V3_47_MIGRATION_LOCK_NOT_ACQUIRED'
);
PREPARE back_record_production_lock_guard FROM @back_record_production_lock_guard_sql;
EXECUTE back_record_production_lock_guard;
DEALLOCATE PREPARE back_record_production_lock_guard;

START TRANSACTION;

SET @has_production_result = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
    AND column_name = 'production_result'
);
SET @sql = IF(@has_production_result = 0,
  'ALTER TABLE biz_finish_roll ADD COLUMN production_result TINYINT DEFAULT NULL COMMENT ''1计划产出 2正常产出 3计划未产出 4实际新增产出''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_production_reason = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
    AND column_name = 'production_adjustment_reason'
);
SET @sql = IF(@has_production_reason = 0,
  'ALTER TABLE biz_finish_roll ADD COLUMN production_adjustment_reason VARCHAR(255) DEFAULT NULL COMMENT ''回录产出调整原因''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_production_index = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
    AND index_name = 'idx_finish_production_result'
);
SET @sql = IF(@has_production_index = 0,
  'CREATE INDEX idx_finish_production_result ON biz_finish_roll (order_uuid, production_result, finish_status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

COMMIT;
SELECT RELEASE_LOCK('paper_mes_back_record_production_result') INTO @back_record_production_unlock;
