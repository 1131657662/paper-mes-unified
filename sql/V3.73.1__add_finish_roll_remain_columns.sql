-- V3.73.1: keep the FinishRoll projection compatible with the estimated-weight release.
-- The columns are additive and guarded so a partially repaired test database is safe to retry.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @has_ownership_status := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
    AND column_name = 'ownership_status'
);
SET @add_ownership_status = IF(
  @has_ownership_status = 0,
  'ALTER TABLE biz_finish_roll ADD COLUMN ownership_status TINYINT NOT NULL DEFAULT 0 COMMENT ''0客户所有 1客户/我方分属 2我方所有'' AFTER is_remain',
  'DO 0'
);
PREPARE add_ownership_status_stmt FROM @add_ownership_status;
EXECUTE add_ownership_status_stmt;
DEALLOCATE PREPARE add_ownership_status_stmt;

SET @has_remain_own_weight := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
    AND column_name = 'remain_own_weight'
);
SET @add_remain_own_weight = IF(
  @has_remain_own_weight = 0,
  'ALTER TABLE biz_finish_roll ADD COLUMN remain_own_weight DECIMAL(10,3) NOT NULL DEFAULT 0.000 COMMENT ''已转入我方的系统重量kg'' AFTER remaining_weight',
  'DO 0'
);
PREPARE add_remain_own_weight_stmt FROM @add_remain_own_weight;
EXECUTE add_remain_own_weight_stmt;
DEALLOCATE PREPARE add_remain_own_weight_stmt;

SET @has_remain_transfer_state := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
    AND column_name = 'remain_transfer_state'
);
SET @add_remain_transfer_state = IF(
  @has_remain_transfer_state = 0,
  'ALTER TABLE biz_finish_roll ADD COLUMN remain_transfer_state TINYINT NOT NULL DEFAULT 0 COMMENT ''0未转让 1部分转让 2全部转让 3部分恢复'' AFTER ownership_status',
  'DO 0'
);
PREPARE add_remain_transfer_state_stmt FROM @add_remain_transfer_state;
EXECUTE add_remain_transfer_state_stmt;
DEALLOCATE PREPARE add_remain_transfer_state_stmt;

SET @has_ownership_index := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
    AND index_name = 'idx_finish_ownership'
);
SET @add_ownership_index = IF(
  @has_ownership_index = 0,
  'ALTER TABLE biz_finish_roll ADD KEY idx_finish_ownership (is_remain, ownership_status, finish_status, is_deleted)',
  'DO 0'
);
PREPARE add_ownership_index_stmt FROM @add_ownership_index;
EXECUTE add_ownership_index_stmt;
DEALLOCATE PREPARE add_ownership_index_stmt;

SET @has_ownership_check := (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'biz_finish_roll'
    AND constraint_name = 'chk_finish_ownership_status'
);
SET @add_ownership_check = IF(
  @has_ownership_check = 0,
  'ALTER TABLE biz_finish_roll ADD CONSTRAINT chk_finish_ownership_status CHECK (ownership_status IN (0, 1, 2))',
  'DO 0'
);
PREPARE add_ownership_check_stmt FROM @add_ownership_check;
EXECUTE add_ownership_check_stmt;
DEALLOCATE PREPARE add_ownership_check_stmt;

SET @has_remain_weight_check := (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'biz_finish_roll'
    AND constraint_name = 'chk_finish_remain_own_weight'
);
SET @add_remain_weight_check = IF(
  @has_remain_weight_check = 0,
  'ALTER TABLE biz_finish_roll ADD CONSTRAINT chk_finish_remain_own_weight CHECK (remain_own_weight >= 0)',
  'DO 0'
);
PREPARE add_remain_weight_check_stmt FROM @add_remain_weight_check;
EXECUTE add_remain_weight_check_stmt;
DEALLOCATE PREPARE add_remain_weight_check_stmt;
