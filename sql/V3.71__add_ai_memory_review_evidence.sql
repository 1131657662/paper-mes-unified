-- V3.71: business-readable AI memory review evidence and manual-final examples.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_ai_memory_review_evidence', 10) INTO @ai_memory_review_lock;
SET @ai_memory_review_guard_sql = IF(
  @ai_memory_review_lock = 1,
  'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''V3.71 migration lock not acquired'''
);
PREPARE ai_memory_review_guard FROM @ai_memory_review_guard_sql;
EXECUTE ai_memory_review_guard;
DEALLOCATE PREPARE ai_memory_review_guard;

ALTER TABLE `biz_project_memory_candidate_evidence`
  MODIFY COLUMN `parse_id` VARCHAR(64) DEFAULT NULL,
  ADD COLUMN `source_type` VARCHAR(24) NOT NULL DEFAULT 'AI_CONFIRMED' AFTER `evidence_hash`,
  ADD COLUMN `phrase` VARCHAR(2000) DEFAULT NULL AFTER `source_type`,
  ADD COLUMN `context_json` JSON DEFAULT NULL AFTER `phrase`,
  ADD COLUMN `proposed_value_json` JSON DEFAULT NULL AFTER `context_json`,
  ADD COLUMN `final_value_json` JSON DEFAULT NULL AFTER `proposed_value_json`,
  ADD COLUMN `difference_json` JSON DEFAULT NULL AFTER `final_value_json`,
  ADD COLUMN `preview_ready` TINYINT DEFAULT NULL AFTER `difference_json`,
  ADD COLUMN `created_by` VARCHAR(64) DEFAULT NULL AFTER `preview_ready`,
  ADD CONSTRAINT `chk_memory_evidence_source_type`
    CHECK (`source_type` IN ('AI_CONFIRMED', 'MANUAL_FINAL')),
  ADD CONSTRAINT `chk_memory_evidence_preview_ready`
    CHECK (`preview_ready` IS NULL OR `preview_ready` IN (0, 1));

SELECT RELEASE_LOCK('paper_mes_ai_memory_review_evidence') INTO @ai_memory_review_unlock;
