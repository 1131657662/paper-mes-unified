-- V3.75: AI process dialogue v2, deterministic preview identity, and redacted memory evidence.
-- The migration is additive and guarded so it can be re-run safely after an interrupted deploy.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_ai_process_dialogue_v2', 10) INTO @ai_dialogue_v2_lock;
SET @ai_dialogue_v2_guard_sql = IF(
  @ai_dialogue_v2_lock = 1,
  'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''V3.75 migration lock not acquired'''
);
PREPARE ai_dialogue_v2_guard FROM @ai_dialogue_v2_guard_sql;
EXECUTE ai_dialogue_v2_guard;
DEALLOCATE PREPARE ai_dialogue_v2_guard;

SET @ai_parse_table := (
  SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
);
SET @ai_parse_guard_sql = IF(
  @ai_parse_table = 1,
  'DO 0',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''biz_process_ai_parse is missing'''
);
PREPARE ai_parse_guard FROM @ai_parse_guard_sql;
EXECUTE ai_parse_guard;
DEALLOCATE PREPARE ai_parse_guard;

SET @add_clarification_round = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_conversation'
    AND column_name = 'clarification_round') = 0,
  'ALTER TABLE biz_process_ai_conversation ADD COLUMN clarification_round TINYINT NOT NULL DEFAULT 0 AFTER last_parse_revision', 'DO 0');
PREPARE add_clarification_round_stmt FROM @add_clarification_round; EXECUTE add_clarification_round_stmt; DEALLOCATE PREPARE add_clarification_round_stmt;
SET @has_clarification_round_check := (SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_ai_conversation'
    AND constraint_name = 'chk_ai_conversation_clarification_round');
SET @add_clarification_round_check = IF(@has_clarification_round_check = 0,
  'ALTER TABLE biz_process_ai_conversation ADD CONSTRAINT chk_ai_conversation_clarification_round CHECK (clarification_round BETWEEN 0 AND 8)', 'DO 0');
PREPARE add_clarification_round_check_stmt FROM @add_clarification_round_check; EXECUTE add_clarification_round_check_stmt; DEALLOCATE PREPARE add_clarification_round_check_stmt;

-- Add one column at a time; this keeps a retry from failing on already-applied columns.
SET @add_dialogue_state = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'dialogue_state') = 0,
  'ALTER TABLE biz_process_ai_parse ADD COLUMN dialogue_state VARCHAR(24) NOT NULL DEFAULT ''PREVIEW_READY'' AFTER status', 'DO 0');
PREPARE add_dialogue_state_stmt FROM @add_dialogue_state; EXECUTE add_dialogue_state_stmt; DEALLOCATE PREPARE add_dialogue_state_stmt;
SET @add_result_kind = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'result_kind') = 0,
  'ALTER TABLE biz_process_ai_parse ADD COLUMN result_kind VARCHAR(16) NOT NULL DEFAULT ''EXTRACTION'' AFTER dialogue_state', 'DO 0');
PREPARE add_result_kind_stmt FROM @add_result_kind; EXECUTE add_result_kind_stmt; DEALLOCATE PREPARE add_result_kind_stmt;
SET @add_workflow_version = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'workflow_version') = 0,
  'ALTER TABLE biz_process_ai_parse ADD COLUMN workflow_version TINYINT NOT NULL DEFAULT 1 AFTER result_kind', 'DO 0');
PREPARE add_workflow_version_stmt FROM @add_workflow_version; EXECUTE add_workflow_version_stmt; DEALLOCATE PREPARE add_workflow_version_stmt;
SET @add_understanding = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'understanding_json') = 0,
  'ALTER TABLE biz_process_ai_parse ADD COLUMN understanding_json JSON DEFAULT NULL AFTER intent_json', 'DO 0');
PREPARE add_understanding_stmt FROM @add_understanding; EXECUTE add_understanding_stmt; DEALLOCATE PREPARE add_understanding_stmt;
SET @add_question = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'question_json') = 0,
  'ALTER TABLE biz_process_ai_parse ADD COLUMN question_json JSON DEFAULT NULL AFTER understanding_json', 'DO 0');
PREPARE add_question_stmt FROM @add_question; EXECUTE add_question_stmt; DEALLOCATE PREPARE add_question_stmt;
SET @add_corrections = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'corrections_json') = 0,
  'ALTER TABLE biz_process_ai_parse ADD COLUMN corrections_json JSON DEFAULT NULL AFTER question_json', 'DO 0');
PREPARE add_corrections_stmt FROM @add_corrections; EXECUTE add_corrections_stmt; DEALLOCATE PREPARE add_corrections_stmt;
SET @add_input_hash = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'input_hash') = 0,
  'ALTER TABLE biz_process_ai_parse ADD COLUMN input_hash CHAR(64) DEFAULT NULL AFTER corrections_json', 'DO 0');
PREPARE add_input_hash_stmt FROM @add_input_hash; EXECUTE add_input_hash_stmt; DEALLOCATE PREPARE add_input_hash_stmt;
SET @add_context_hash = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'context_hash') = 0,
  'ALTER TABLE biz_process_ai_parse ADD COLUMN context_hash CHAR(64) DEFAULT NULL AFTER input_hash', 'DO 0');
PREPARE add_context_hash_stmt FROM @add_context_hash; EXECUTE add_context_hash_stmt; DEALLOCATE PREPARE add_context_hash_stmt;
SET @add_preview_hash = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'preview_hash') = 0,
  'ALTER TABLE biz_process_ai_parse ADD COLUMN preview_hash CHAR(64) DEFAULT NULL AFTER context_hash', 'DO 0');
PREPARE add_preview_hash_stmt FROM @add_preview_hash; EXECUTE add_preview_hash_stmt; DEALLOCATE PREPARE add_preview_hash_stmt;
SET @add_failure_code = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'failure_code') = 0,
  'ALTER TABLE biz_process_ai_parse ADD COLUMN failure_code VARCHAR(64) DEFAULT NULL AFTER preview_hash', 'DO 0');
PREPARE add_failure_code_stmt FROM @add_failure_code; EXECUTE add_failure_code_stmt; DEALLOCATE PREPARE add_failure_code_stmt;
SET @add_failure_trace = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'failure_trace_id') = 0,
  'ALTER TABLE biz_process_ai_parse ADD COLUMN failure_trace_id VARCHAR(64) DEFAULT NULL AFTER failure_code', 'DO 0');
PREPARE add_failure_trace_stmt FROM @add_failure_trace; EXECUTE add_failure_trace_stmt; DEALLOCATE PREPARE add_failure_trace_stmt;
SET @add_required_defaults = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'required_default_ids') = 0,
  'ALTER TABLE biz_process_ai_parse ADD COLUMN required_default_ids JSON DEFAULT NULL AFTER failure_trace_id', 'DO 0');
PREPARE add_required_defaults_stmt FROM @add_required_defaults; EXECUTE add_required_defaults_stmt; DEALLOCATE PREPARE add_required_defaults_stmt;
SET @add_ack_defaults = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND column_name = 'acknowledged_default_ids') = 0,
  'ALTER TABLE biz_process_ai_parse ADD COLUMN acknowledged_default_ids JSON DEFAULT NULL AFTER required_default_ids', 'DO 0');
PREPARE add_ack_defaults_stmt FROM @add_ack_defaults; EXECUTE add_ack_defaults_stmt; DEALLOCATE PREPARE add_ack_defaults_stmt;

ALTER TABLE biz_process_ai_parse MODIFY COLUMN intent_json JSON DEFAULT NULL;

SET @has_dialogue_check := (SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND constraint_name = 'chk_ai_parse_dialogue_state');
SET @add_dialogue_check = IF(@has_dialogue_check = 0,
  'ALTER TABLE biz_process_ai_parse ADD CONSTRAINT chk_ai_parse_dialogue_state CHECK (dialogue_state IN (''UNDERSTANDING'', ''CLARIFYING'', ''PREVIEW_READY'', ''REVISING'', ''FAILED'', ''COMPLETED''))', 'DO 0');
PREPARE add_dialogue_check_stmt FROM @add_dialogue_check; EXECUTE add_dialogue_check_stmt; DEALLOCATE PREPARE add_dialogue_check_stmt;
SET @has_result_check := (SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND constraint_name = 'chk_ai_parse_result_kind');
SET @add_result_check = IF(@has_result_check = 0,
  'ALTER TABLE biz_process_ai_parse ADD CONSTRAINT chk_ai_parse_result_kind CHECK (result_kind IN (''EXTRACTION'', ''UNDERSTANDING'', ''FAILURE''))', 'DO 0');
PREPARE add_result_check_stmt FROM @add_result_check; EXECUTE add_result_check_stmt; DEALLOCATE PREPARE add_result_check_stmt;
SET @has_workflow_check := (SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND constraint_name = 'chk_ai_parse_workflow_version');
SET @add_workflow_check = IF(@has_workflow_check = 0,
  'ALTER TABLE biz_process_ai_parse ADD CONSTRAINT chk_ai_parse_workflow_version CHECK (workflow_version IN (1, 2))', 'DO 0');
PREPARE add_workflow_check_stmt FROM @add_workflow_check; EXECUTE add_workflow_check_stmt; DEALLOCATE PREPARE add_workflow_check_stmt;

-- Normalize legacy rows before the result-consistency CHECK is added.  Legacy rows
-- without a stored intent are failures, never extraction candidates.
UPDATE biz_process_ai_parse
SET result_kind = CASE
      WHEN intent_json IS NOT NULL THEN 'EXTRACTION'
      WHEN understanding_json IS NOT NULL THEN 'UNDERSTANDING'
      ELSE 'FAILURE'
    END,
    failure_code = CASE
      WHEN intent_json IS NULL AND understanding_json IS NULL
        THEN COALESCE(failure_code,
          CASE WHEN status = 'INTERRUPTED' THEN 'LEGACY_INTERRUPTED'
               ELSE 'LEGACY_UNCLASSIFIED' END)
      ELSE failure_code
    END,
    dialogue_state = CASE
      WHEN status = 'CONFIRMED' THEN 'COMPLETED'
      WHEN status IN ('REJECTED', 'EXPIRED') THEN 'COMPLETED'
      WHEN status = 'INTERRUPTED' THEN 'FAILED'
      WHEN status = 'CLARIFICATION' THEN 'CLARIFYING'
      WHEN understanding_json IS NOT NULL THEN 'CLARIFYING'
      WHEN intent_json IS NOT NULL THEN 'PREVIEW_READY'
      ELSE 'FAILED'
    END
WHERE workflow_version = 1;

SET @has_result_consistency := (SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND constraint_name = 'chk_ai_parse_result_consistency');
SET @add_result_consistency = IF(@has_result_consistency = 0,
  'ALTER TABLE biz_process_ai_parse ADD CONSTRAINT chk_ai_parse_result_consistency CHECK ((result_kind = ''EXTRACTION'' AND intent_json IS NOT NULL) OR (result_kind = ''UNDERSTANDING'' AND understanding_json IS NOT NULL) OR (result_kind = ''FAILURE'' AND intent_json IS NULL AND understanding_json IS NULL AND failure_code IS NOT NULL))', 'DO 0');
PREPARE add_result_consistency_stmt FROM @add_result_consistency; EXECUTE add_result_consistency_stmt; DEALLOCATE PREPARE add_result_consistency_stmt;

SET @has_parse_dialogue_index := (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_process_ai_parse'
    AND index_name = 'idx_ai_parse_conversation_dialogue');
SET @add_parse_dialogue_index = IF(@has_parse_dialogue_index = 0,
  'ALTER TABLE biz_process_ai_parse ADD KEY idx_ai_parse_conversation_dialogue (conversation_id, dialogue_state, created_at)', 'DO 0');
PREPARE add_parse_dialogue_index_stmt FROM @add_parse_dialogue_index; EXECUTE add_parse_dialogue_index_stmt; DEALLOCATE PREPARE add_parse_dialogue_index_stmt;

-- Evidence is no longer allowed to keep orders alive. HMAC reference hashes are populated by the application.
SET @has_order_ref_hash := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_project_memory_candidate_evidence'
    AND column_name = 'order_ref_hash');
SET @add_order_ref_hash = IF(@has_order_ref_hash = 0,
  'ALTER TABLE biz_project_memory_candidate_evidence ADD COLUMN order_ref_hash CHAR(64) DEFAULT NULL AFTER order_uuid', 'DO 0');
PREPARE add_order_ref_hash_stmt FROM @add_order_ref_hash; EXECUTE add_order_ref_hash_stmt; DEALLOCATE PREPARE add_order_ref_hash_stmt;
SET @has_parse_ref_hash := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_project_memory_candidate_evidence'
    AND column_name = 'parse_ref_hash');
SET @add_parse_ref_hash = IF(@has_parse_ref_hash = 0,
  'ALTER TABLE biz_project_memory_candidate_evidence ADD COLUMN parse_ref_hash CHAR(64) DEFAULT NULL AFTER parse_id', 'DO 0');
PREPARE add_parse_ref_hash_stmt FROM @add_parse_ref_hash; EXECUTE add_parse_ref_hash_stmt; DEALLOCATE PREPARE add_parse_ref_hash_stmt;
SET @has_audit_ciphertext := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_project_memory_candidate_evidence'
    AND column_name = 'audit_context_ciphertext');
SET @add_audit_ciphertext = IF(@has_audit_ciphertext = 0,
  'ALTER TABLE biz_project_memory_candidate_evidence ADD COLUMN audit_context_ciphertext MEDIUMTEXT DEFAULT NULL AFTER context_json', 'DO 0');
PREPARE add_audit_ciphertext_stmt FROM @add_audit_ciphertext; EXECUTE add_audit_ciphertext_stmt; DEALLOCATE PREPARE add_audit_ciphertext_stmt;
SET @has_audit_hash := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_project_memory_candidate_evidence'
    AND column_name = 'audit_context_hash');
SET @add_audit_hash = IF(@has_audit_hash = 0,
  'ALTER TABLE biz_project_memory_candidate_evidence ADD COLUMN audit_context_hash CHAR(64) DEFAULT NULL AFTER audit_context_ciphertext', 'DO 0');
PREPARE add_audit_hash_stmt FROM @add_audit_hash; EXECUTE add_audit_hash_stmt; DEALLOCATE PREPARE add_audit_hash_stmt;
ALTER TABLE biz_project_memory_candidate_evidence
  MODIFY COLUMN order_uuid VARCHAR(36) DEFAULT NULL,
  MODIFY COLUMN parse_id VARCHAR(64) DEFAULT NULL;

SET @has_order_fk := (SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'biz_project_memory_candidate_evidence'
    AND constraint_name = 'fk_memory_evidence_order');
SET @drop_order_fk = IF(@has_order_fk = 1,
  'ALTER TABLE biz_project_memory_candidate_evidence DROP FOREIGN KEY fk_memory_evidence_order', 'DO 0');
PREPARE drop_order_fk_stmt FROM @drop_order_fk; EXECUTE drop_order_fk_stmt; DEALLOCATE PREPARE drop_order_fk_stmt;
SET @has_parse_fk := (SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'biz_project_memory_candidate_evidence'
    AND constraint_name = 'fk_memory_evidence_parse');
SET @drop_parse_fk = IF(@has_parse_fk = 1,
  'ALTER TABLE biz_project_memory_candidate_evidence DROP FOREIGN KEY fk_memory_evidence_parse', 'DO 0');
PREPARE drop_parse_fk_stmt FROM @drop_parse_fk; EXECUTE drop_parse_fk_stmt; DEALLOCATE PREPARE drop_parse_fk_stmt;
SET @has_candidate_order_key := (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_project_memory_candidate_evidence'
    AND index_name = 'uk_memory_candidate_order');
SET @drop_candidate_order_key = IF(@has_candidate_order_key = 1,
  'ALTER TABLE biz_project_memory_candidate_evidence DROP INDEX uk_memory_candidate_order', 'DO 0');
PREPARE drop_candidate_order_key_stmt FROM @drop_candidate_order_key; EXECUTE drop_candidate_order_key_stmt; DEALLOCATE PREPARE drop_candidate_order_key_stmt;
SET @has_candidate_ref_key := (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_project_memory_candidate_evidence'
    AND index_name = 'uk_memory_candidate_order_ref');
SET @add_candidate_ref_key = IF(@has_candidate_ref_key = 0,
  'ALTER TABLE biz_project_memory_candidate_evidence ADD UNIQUE KEY uk_memory_candidate_order_ref (candidate_uuid, order_ref_hash)', 'DO 0');
PREPARE add_candidate_ref_key_stmt FROM @add_candidate_ref_key; EXECUTE add_candidate_ref_key_stmt; DEALLOCATE PREPARE add_candidate_ref_key_stmt;
-- Re-add the nullable foreign keys only when the previous attempt did not reach
-- this point. This makes a retry after a timeout safe instead of failing with a
-- duplicate constraint name.
SET @has_order_fk_after_drop := (SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'biz_project_memory_candidate_evidence'
    AND constraint_name = 'fk_memory_evidence_order');
SET @add_order_fk = IF(@has_order_fk_after_drop = 0,
  'ALTER TABLE biz_project_memory_candidate_evidence ADD CONSTRAINT fk_memory_evidence_order FOREIGN KEY (order_uuid) REFERENCES biz_process_order (uuid) ON DELETE SET NULL ON UPDATE RESTRICT', 'DO 0');
PREPARE add_order_fk_stmt FROM @add_order_fk; EXECUTE add_order_fk_stmt; DEALLOCATE PREPARE add_order_fk_stmt;
SET @has_parse_fk_after_drop := (SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'biz_project_memory_candidate_evidence'
    AND constraint_name = 'fk_memory_evidence_parse');
SET @add_parse_fk = IF(@has_parse_fk_after_drop = 0,
  'ALTER TABLE biz_project_memory_candidate_evidence ADD CONSTRAINT fk_memory_evidence_parse FOREIGN KEY (parse_id) REFERENCES biz_process_ai_parse (parse_id) ON DELETE SET NULL ON UPDATE RESTRICT', 'DO 0');
PREPARE add_parse_fk_stmt FROM @add_parse_fk; EXECUTE add_parse_fk_stmt; DEALLOCATE PREPARE add_parse_fk_stmt;

-- Historical evidence cannot be safely HMAC-ed or encrypted inside MySQL because the
-- application-only keys must never be copied into a migration session. Keep the legacy
-- values until the application audit backfill encrypts the minimum audit context, creates
-- HMAC references, and then clears the plaintext. Candidate learning remains fail-closed
-- until both backfills finish, so this short transition cannot create new shared knowledge.
-- The derived table inherits the live column collation and avoids requiring the
-- CREATE TEMPORARY TABLES privilege from the production migration account.
UPDATE biz_project_memory_candidate AS candidate
JOIN (
  SELECT DISTINCT candidate_uuid
  FROM biz_project_memory_candidate_evidence
  WHERE order_uuid IS NOT NULL OR parse_id IS NOT NULL
     OR phrase IS NOT NULL OR context_json IS NOT NULL
     OR (audit_context_hash IS NULL AND (
          proposed_value_json IS NOT NULL
          OR final_value_json IS NOT NULL
          OR difference_json IS NOT NULL
        ))
) AS legacy
  ON legacy.candidate_uuid = candidate.uuid
SET candidate.distinct_order_count = 0,
    candidate.status = CASE WHEN candidate.status = 'READY' THEN 'CANDIDATE'
                            ELSE candidate.status END
WHERE candidate.status <> 'ACTIVE';

SELECT RELEASE_LOCK('paper_mes_ai_process_dialogue_v2') INTO @ai_dialogue_v2_unlock;
