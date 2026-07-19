-- V3.17: support the historical unassigned-finish governance query.
SET @index_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_finish_roll'
    AND index_name = 'idx_finish_unassigned_order'
);
SET @sql := IF(@index_missing,
  'ALTER TABLE `biz_finish_roll` ADD INDEX `idx_finish_unassigned_order` (`is_deleted`, `finish_status`, `warehouse_uuid`, `order_uuid`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
