-- V2.1: 结算明细按加工单唯一，防止同一加工单进入多张有效结算单。
-- 执行前若存在历史重复数据，本脚本会主动报错中止，需要先人工核对并作废多余结算单。

SET @duplicate_order_count := (
  SELECT COUNT(*)
  FROM (
    SELECT order_uuid
    FROM biz_settle_detail
    WHERE is_deleted = 0
    GROUP BY order_uuid
    HAVING COUNT(*) > 1
  ) duplicated
);

SET @duplicate_guard_sql := IF(
  @duplicate_order_count > 0,
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''存在重复结算加工单，请先清理 biz_settle_detail 后再添加唯一约束''',
  'SELECT 1'
);
PREPARE duplicate_guard FROM @duplicate_guard_sql;
EXECUTE duplicate_guard;
DEALLOCATE PREPARE duplicate_guard;

SET @active_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_settle_detail'
    AND column_name = 'active_order_uuid'
);

SET @add_active_column_sql := IF(
  @active_column_exists = 0,
  'ALTER TABLE biz_settle_detail ADD COLUMN active_order_uuid VARCHAR(36) GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN order_uuid ELSE NULL END) STORED COMMENT ''有效结算加工单唯一键''',
  'SELECT 1'
);
PREPARE add_active_column_stmt FROM @add_active_column_sql;
EXECUTE add_active_column_stmt;
DEALLOCATE PREPARE add_active_column_stmt;

SET @constraint_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_settle_detail'
    AND index_name = 'uk_settle_detail_order_active'
);

SET @add_constraint_sql := IF(
  @constraint_exists = 0,
  'ALTER TABLE biz_settle_detail ADD UNIQUE KEY uk_settle_detail_order_active (active_order_uuid)',
  'SELECT 1'
);
PREPARE add_constraint_stmt FROM @add_constraint_sql;
EXECUTE add_constraint_stmt;
DEALLOCATE PREPARE add_constraint_stmt;
