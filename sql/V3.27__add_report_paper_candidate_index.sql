-- V3.27: support indexed prefix lookup for report paper candidates.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @index_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_original_roll'
    AND index_name = 'idx_original_roll_paper_candidate'
);
SET @sql := IF(
  @index_missing,
  'ALTER TABLE `biz_original_roll` ADD INDEX `idx_original_roll_paper_candidate` (`is_deleted`, `paper_name`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
