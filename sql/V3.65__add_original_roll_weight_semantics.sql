-- V3.65: distinguish unknown/estimated/measured source weight and billing freshness.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_original_roll_weight_semantics', 10) INTO @weight_semantics_lock;
SET @weight_semantics_guard_sql = IF(@weight_semantics_lock = 1, 'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''V3.65 migration lock not acquired''');
PREPARE weight_semantics_guard FROM @weight_semantics_guard_sql;
EXECUTE weight_semantics_guard;
DEALLOCATE PREPARE weight_semantics_guard;

SET @sql := 'ALTER TABLE biz_original_roll MODIFY COLUMN roll_weight DECIMAL(10,3) DEFAULT NULL COMMENT ''标称/估算单件重量 kg''';
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := 'ALTER TABLE biz_original_roll MODIFY COLUMN total_weight DECIMAL(10,3) DEFAULT NULL COMMENT ''标称/估算总重 kg''';
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_weight_status := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_original_roll' AND column_name = 'weight_status');
SET @sql := IF(@has_weight_status = 0,
  'ALTER TABLE biz_original_roll ADD COLUMN weight_status VARCHAR(16) NOT NULL DEFAULT ''ESTIMATED'' COMMENT ''UNKNOWN/ESTIMATED/MEASURED'' AFTER total_weight',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_weight_source := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_original_roll' AND column_name = 'weight_source');
SET @sql := IF(@has_weight_source = 0,
  'ALTER TABLE biz_original_roll ADD COLUMN weight_source VARCHAR(16) DEFAULT NULL COMMENT ''MANUAL/SCALE/IMPORT/INFERRED/LEGACY/MANUAL_CONFIRM/CARRIED_NOMINAL/MANUAL_ESTIMATE'' AFTER weight_status',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_weight_recorded_at := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_original_roll' AND column_name = 'weight_recorded_at');
SET @sql := IF(@has_weight_recorded_at = 0,
  'ALTER TABLE biz_original_roll ADD COLUMN weight_recorded_at DATETIME DEFAULT NULL AFTER weight_source',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_weight_recorded_by := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_original_roll' AND column_name = 'weight_recorded_by');
SET @sql := IF(@has_weight_recorded_by = 0,
  'ALTER TABLE biz_original_roll ADD COLUMN weight_recorded_by VARCHAR(50) DEFAULT NULL AFTER weight_recorded_at',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE biz_original_roll SET weight_status = CASE
  WHEN actual_weight IS NOT NULL AND actual_weight > 0 THEN 'MEASURED'
  WHEN roll_weight IS NULL OR roll_weight <= 0 THEN 'UNKNOWN'
  ELSE 'ESTIMATED' END,
  weight_source = CASE WHEN actual_weight IS NOT NULL AND actual_weight > 0 THEN 'LEGACY'
    WHEN roll_weight IS NULL OR roll_weight <= 0 THEN NULL ELSE 'LEGACY' END
WHERE weight_status = 'ESTIMATED' OR weight_status IS NULL;

SET @append_roll_exists := (SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_order_append_roll');
SET @sql := IF(@append_roll_exists = 1,
  'ALTER TABLE biz_process_order_append_roll MODIFY COLUMN roll_weight DECIMAL(12,3) DEFAULT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @append_weight_status := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_order_append_roll' AND column_name = 'weight_status');
SET @sql := IF(@append_roll_exists = 1 AND @append_weight_status = 0,
  'ALTER TABLE biz_process_order_append_roll ADD COLUMN weight_status VARCHAR(16) DEFAULT ''ESTIMATED'' AFTER roll_weight',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_consume_ratio := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_finish_original_rel' AND column_name = 'consume_ratio');
SET @sql := IF(@has_consume_ratio = 0,
  'ALTER TABLE biz_finish_original_rel ADD COLUMN consume_ratio DECIMAL(5,2) DEFAULT NULL COMMENT ''Source consumption ratio %'' AFTER share_ratio',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_billing_status := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_step' AND column_name = 'billing_weight_status');
SET @sql := IF(@has_billing_status = 0,
  'ALTER TABLE biz_process_step ADD COLUMN billing_weight_status VARCHAR(16) DEFAULT NULL COMMENT ''PENDING/ESTIMATED/MEASURED/BLOCKED'' AFTER process_weight',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_billing_basis := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_step' AND column_name = 'billing_weight_basis');
SET @sql := IF(@has_billing_basis = 0,
  'ALTER TABLE biz_process_step ADD COLUMN billing_weight_basis VARCHAR(32) DEFAULT NULL AFTER billing_weight_status',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_pricing_dirty := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_step' AND column_name = 'pricing_dirty');
SET @sql := IF(@has_pricing_dirty = 0,
  'ALTER TABLE biz_process_step ADD COLUMN pricing_dirty TINYINT NOT NULL DEFAULT 0 COMMENT ''1 requires fee recalculation'' AFTER billing_weight_basis',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE biz_process_step step
JOIN biz_process_order process_order ON process_order.uuid = step.order_uuid
LEFT JOIN biz_original_roll roll ON roll.uuid = step.original_uuid
SET step.billing_weight_status = CASE
      WHEN step.step_type <> 2 THEN step.billing_weight_status
      WHEN COALESCE(step.billing_mode, 1) <> 1 THEN NULL
      WHEN roll.actual_weight IS NOT NULL AND roll.actual_weight > 0
        AND NOT EXISTS (
          SELECT 1
          FROM biz_finish_original_rel anchor_rel
          JOIN biz_finish_original_rel source_rel
            ON source_rel.finish_uuid = anchor_rel.finish_uuid
           AND source_rel.order_uuid = anchor_rel.order_uuid
           AND source_rel.is_deleted = 0
          JOIN biz_original_roll source_roll ON source_roll.uuid = source_rel.original_uuid
          WHERE anchor_rel.order_uuid = step.order_uuid
            AND anchor_rel.original_uuid = step.original_uuid
            AND anchor_rel.is_deleted = 0
            AND (source_roll.actual_weight IS NULL OR source_roll.actual_weight <= 0)
        ) THEN 'MEASURED'
      WHEN step.process_weight IS NOT NULL AND step.process_weight > 0 THEN 'ESTIMATED'
      ELSE 'PENDING'
    END,
    step.billing_weight_basis = CASE
      WHEN step.step_type = 2 AND COALESCE(step.billing_mode, 1) = 1 THEN 'INPUT_TOTAL'
      WHEN step.step_type = 2 THEN 'FIXED'
      ELSE step.billing_weight_basis
    END,
    step.pricing_dirty = CASE
      WHEN step.step_type = 2 AND COALESCE(step.billing_mode, 1) = 1
        AND (step.process_weight IS NULL OR step.process_weight <= 0) THEN 1
      ELSE step.pricing_dirty
    END
WHERE step.step_type = 2
  AND step.billing_weight_status IS NULL
  AND process_order.order_status BETWEEN 0 AND 3;

SELECT RELEASE_LOCK('paper_mes_original_roll_weight_semantics') INTO @weight_semantics_unlock;
