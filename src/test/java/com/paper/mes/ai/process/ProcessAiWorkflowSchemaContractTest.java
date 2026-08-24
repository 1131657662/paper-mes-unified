package com.paper.mes.ai.process;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiWorkflowSchemaContractTest {

    @Test
    void migrationCreatesOrderScopedConversationAndParseTables() throws IOException {
        String migration = read("sql/V3.69__add_ai_process_workflow.sql");

        assertThat(migration).contains(
                "V3.69 migration lock not acquired",
                "ai_requirement_json",
                "ai_intent_json",
                "CREATE TABLE IF NOT EXISTS `biz_process_ai_conversation`",
                "CREATE TABLE IF NOT EXISTS `biz_process_ai_message`",
                "CREATE TABLE IF NOT EXISTS `biz_process_ai_parse`",
                "CREATE TABLE IF NOT EXISTS `sys_ai_call_audit`",
                "CREATE TABLE IF NOT EXISTS `sys_ai_provider_secret`",
                "uk_ai_parse_request_idempotency",
                "uk_ai_parse_apply_idempotency",
                "confirmed_result_json",
                "chk_ai_parse_confirmation",
                "SELECT RELEASE_LOCK");
    }

    @Test
    void canonicalBaselineMatchesTheProcessAiMigration() throws IOException {
        assertThat(read("sql/schema-baseline.version").trim()).isEqualTo("3.76");
        assertThat(read("sql/01_schema_v4.1.sql")).contains(
                "-- V3.69 canonical baseline",
                "-- V3.71 canonical baseline",
                "CREATE TABLE `biz_process_ai_conversation`",
                "CREATE TABLE `biz_process_ai_parse`",
                "confirmed_result_json",
                "CREATE TABLE `sys_ai_call_audit`",
                "CREATE TABLE `sys_ai_provider_secret`",
                "CREATE TABLE `biz_project_memory_candidate`",
                "CREATE TABLE `biz_project_memory_candidate_evidence`",
                "`review_notes` VARCHAR(500)",
                "`source_type` VARCHAR(24)",
                "`final_value_json` JSON",
                "CONSTRAINT `fk_memory_evidence_parse`",
                "KEY `idx_ai_audit_attempt`");
    }

    @Test
    void messageIdempotencyIsScopedToTheMemoryGeneration() throws IOException {
        String migration = read("sql/V3.76__scope_ai_message_idempotency_to_memory_generation.sql");
        String baseline = read("sql/01_schema_v4.1.sql");

        assertThat(migration).contains(
                "V3.76 migration lock not acquired",
                "DROP INDEX uk_ai_message_idempotency",
                "uk_ai_message_idempotency_generation",
                "conversation_id, memory_generation, idempotency_key",
                "SELECT RELEASE_LOCK");
        assertThat(baseline).contains(
                "UNIQUE KEY `uk_ai_message_idempotency_generation` (`conversation_id`, `memory_generation`, `idempotency_key`)");
    }

    @Test
    void reviewEvidenceMigrationSupportsManualFinalExamples() throws IOException {
        assertThat(read("sql/V3.71__add_ai_memory_review_evidence.sql")).contains(
                "MODIFY COLUMN `parse_id` VARCHAR(64) DEFAULT NULL",
                "`source_type` VARCHAR(24)",
                "`final_value_json` JSON",
                "'MANUAL_FINAL'",
                "SELECT RELEASE_LOCK");
    }

    @Test
    void hardeningMigrationKeepsCandidateReviewAndParseEvidenceContracts() throws IOException {
        assertThat(read("sql/V3.70__harden_ai_process_assistant.sql")).contains(
                "CREATE TABLE IF NOT EXISTS `biz_project_memory_candidate`",
                "`review_notes` VARCHAR(500)",
                "CREATE TABLE IF NOT EXISTS `biz_project_memory_candidate_evidence`",
                "CONSTRAINT `fk_memory_evidence_parse`",
                "REFERENCES `biz_process_ai_parse` (`parse_id`)");
    }

    @Test
    void dialogueV2MigrationAddsDualStateAndRedactedEvidenceContracts() throws IOException {
        String migration = read("sql/V3.75__add_ai_process_dialogue_v2.sql");
        String repository = read("src/main/java/com/paper/mes/ai/process/parse/ProcessAiParseRepository.java");
        String candidateRepository = read(
                "src/main/java/com/paper/mes/ai/memory/candidate/ProjectMemoryCandidateRepository.java");

        assertThat(migration).contains(
                "V3.75 migration lock not acquired",
                "dialogue_state",
                "result_kind",
                "workflow_version",
                "understanding_json",
                "preview_hash",
                "chk_ai_parse_result_consistency",
                "order_ref_hash",
                "parse_ref_hash",
                "audit_context_ciphertext",
                "ON DELETE SET NULL",
                "MODIFY COLUMN order_uuid VARCHAR(36) DEFAULT NULL",
                "MODIFY COLUMN parse_id VARCHAR(64) DEFAULT NULL",
                "ADD UNIQUE KEY uk_memory_candidate_order_ref (candidate_uuid, order_ref_hash)",
                "SELECT RELEASE_LOCK");
        assertThat(migration).contains("application audit backfill")
                .doesNotContain("context_json = NULL", "phrase = NULL");
        assertThat(migration).contains(
                "proposed_value_json IS NOT NULL",
                "final_value_json IS NOT NULL",
                "difference_json IS NOT NULL",
                "audit_context_hash IS NULL AND (");
        assertThat(repository).contains("question_json = CAST(? AS JSON)");
        assertThat(candidateRepository).contains(
                "WHERE audit_context_hash IS NULL",
                "AND audit_context_hash IS NULL");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
