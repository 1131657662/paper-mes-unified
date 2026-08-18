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
        assertThat(read("sql/schema-baseline.version").trim()).isEqualTo("3.73");
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

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
