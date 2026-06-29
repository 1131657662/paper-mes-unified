-- Phase 5.1：追加工序功能 - MySQL 可执行主工序唯一约束
-- 用途：防止同一原纸卷存在多个有效主工艺。
-- 说明：MySQL 8 不支持 PostgreSQL 风格的 WHERE 部分唯一索引。
--      这里用生成列把“有效主工序”的 original_uuid 暴露出来，
--      非主工序或软删除记录为 NULL，唯一索引允许多个 NULL。

ALTER TABLE biz_process_step
  ADD COLUMN active_main_original_uuid VARCHAR(36)
    GENERATED ALWAYS AS (
      CASE
        WHEN is_main = 1 AND is_deleted = 0 THEN original_uuid
        ELSE NULL
      END
    ) STORED,
  ADD UNIQUE KEY uk_roll_main_step (active_main_original_uuid);
