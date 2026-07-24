-- V3.30: make the 10-second report query snapshot reuse window concurrency-safe.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @column_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'rpt_report_query_snapshot'
    AND column_name = 'idempotency_bucket'
);
SET @sql := IF(
  @column_missing,
  'ALTER TABLE `rpt_report_query_snapshot` ADD COLUMN `idempotency_bucket` BIGINT NULL AFTER `query_hash`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `rpt_report_query_snapshot`
SET `idempotency_bucket` = FLOOR(UNIX_TIMESTAMP(`create_time`) / 10)
WHERE `idempotency_bucket` IS NULL;

UPDATE `rpt_report_query_snapshot` target
JOIN (
  SELECT `uuid`, duplicate_rank, legacy_sequence
  FROM (
    SELECT `uuid`,
      ROW_NUMBER() OVER (
        PARTITION BY `owner_uuid`, `permission_hash`, `query_hash`,
          `metric_release_uuid`, `idempotency_bucket`
        ORDER BY `create_time`, `uuid`
      ) AS duplicate_rank,
      ROW_NUMBER() OVER (ORDER BY `create_time`, `uuid`) AS legacy_sequence
    FROM `rpt_report_query_snapshot`
  ) ranked_rows
) ranked ON ranked.uuid = target.uuid
SET target.idempotency_bucket = -ranked.legacy_sequence
WHERE ranked.duplicate_rank > 1;

SET @column_nullable := (
  SELECT COUNT(*) > 0
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'rpt_report_query_snapshot'
    AND column_name = 'idempotency_bucket'
    AND is_nullable = 'YES'
);
SET @sql := IF(
  @column_nullable,
  'ALTER TABLE `rpt_report_query_snapshot` MODIFY COLUMN `idempotency_bucket` BIGINT NOT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'rpt_report_query_snapshot'
    AND index_name = 'uk_report_query_snapshot_idempotency'
);
SET @sql := IF(
  @index_missing,
  'ALTER TABLE `rpt_report_query_snapshot` ADD UNIQUE KEY `uk_report_query_snapshot_idempotency` (`owner_uuid`, `permission_hash`, `query_hash`, `metric_release_uuid`, `idempotency_bucket`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
