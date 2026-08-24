package com.paper.mes.ai.process.parse;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class ProcessAiParseRepository {

    private static final String SELECT_COLUMNS = """
             SELECT uuid, order_uuid, conversation_id, parse_id, parse_revision,
                    memory_generation,
                    request_idempotency_key, expected_version, status, provider, model,
                   route, schema_version, project_memory_version, project_memory_checksum,
                   project_memory_item_ids, intent_json, result_hash, apply_idempotency_key,
                   accepted_field_paths, plan_hash, next_version, confirmed_result_json,
                   confirmed_by, confirmed_at, created_at, dialogue_state, result_kind,
                   workflow_version, understanding_json, question_json, corrections_json,
                   input_hash, context_hash, preview_hash, failure_code, failure_trace_id,
                   required_default_ids, acknowledged_default_ids
            FROM biz_process_ai_parse
            """;

    private final JdbcTemplate jdbcTemplate;

    Optional<ProcessAiParseRecord> findByRequestKey(String conversationId, String requestKey) {
        List<ProcessAiParseRecord> rows = jdbcTemplate.query(SELECT_COLUMNS + """
                WHERE conversation_id = ? AND request_idempotency_key = ?
                """, (resultSet, rowNumber) -> map(resultSet), conversationId, requestKey);
        return rows.stream().findFirst();
    }

    Optional<ProcessAiParseRecord> findByParseId(String parseId) {
        List<ProcessAiParseRecord> rows = jdbcTemplate.query(SELECT_COLUMNS + """
                WHERE parse_id = ?
                """, (resultSet, rowNumber) -> map(resultSet), parseId);
        return rows.stream().findFirst();
    }

    Optional<ProcessAiParseRecord> findByParseIdForUpdate(String parseId) {
        List<ProcessAiParseRecord> rows = jdbcTemplate.query(SELECT_COLUMNS + """
                WHERE parse_id = ?
                FOR UPDATE
                """, (resultSet, rowNumber) -> map(resultSet), parseId);
        return rows.stream().findFirst();
    }

    Optional<ProcessAiParseRecord> findLatestClarification(String orderUuid,
                                                           String conversationId,
                                                           int expectedVersion) {
        List<ProcessAiParseRecord> rows = jdbcTemplate.query(SELECT_COLUMNS + """
                WHERE order_uuid = ? AND conversation_id = ? AND expected_version = ?
                  AND dialogue_state IN ('UNDERSTANDING', 'CLARIFYING')
                  AND ((result_kind = 'UNDERSTANDING')
                    OR (result_kind = 'EXTRACTION' AND status = 'CLARIFICATION'))
                ORDER BY parse_revision DESC, created_at DESC
                LIMIT 1
                """, (resultSet, rowNumber) -> map(resultSet), orderUuid,
                conversationId, expectedVersion);
        return rows.stream().findFirst();
    }

    int insert(ProcessAiParseRecord row) {
        return jdbcTemplate.update("""
                INSERT INTO biz_process_ai_parse
                  (uuid, order_uuid, conversation_id, parse_id, parse_revision,
                   memory_generation, request_idempotency_key, expected_version, status, provider, model,
                   model_version, route, schema_version, project_memory_version,
                   project_memory_checksum, project_memory_item_ids, intent_json,
                   dialogue_state, result_kind, workflow_version, understanding_json,
                   question_json, corrections_json, input_hash, context_hash, preview_hash,
                   failure_code, failure_trace_id, required_default_ids,
                   acknowledged_default_ids, result_hash)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?,
                        CAST(? AS JSON), ?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON),
                        CAST(? AS JSON), ?, ?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON), ?)
                """, row.uuid(), row.orderUuid(), row.conversationId(), row.parseId(),
                row.parseRevision(), row.memoryGeneration(), row.requestIdempotencyKey(),
                row.expectedVersion(), row.status(), row.provider(), row.model(),
                row.route(), row.schemaVersion(),
                row.projectMemoryVersion(), row.projectMemoryChecksum(),
                row.projectMemoryItemIds(), row.intentJson(), row.dialogueState(), row.resultKind(),
                row.workflowVersion(), row.understandingJson(), row.questionJson(),
                row.correctionsJson(), row.inputHash(), row.contextHash(), row.previewHash(),
                row.failureCode(), row.failureTraceId(), row.requiredDefaultIds(),
                row.acknowledgedDefaultIds(), row.resultHash());
    }

    int confirm(String parseId, ProcessAiParseConfirmation confirmation) {
        return jdbcTemplate.update("""
                UPDATE biz_process_ai_parse
                SET status = 'CONFIRMED', apply_idempotency_key = ?,
                    accepted_field_paths = CAST(? AS JSON), plan_hash = ?, next_version = ?,
                    confirmed_result_json = CAST(? AS JSON), confirmed_by = ?, confirmed_at = ?,
                    acknowledged_default_ids = CAST(? AS JSON), dialogue_state = 'COMPLETED'
                WHERE parse_id = ? AND status = 'READY'
                """, confirmation.applyIdempotencyKey(), confirmation.acceptedFieldPathsJson(),
                confirmation.planHash(), confirmation.nextVersion(),
                confirmation.confirmedResultJson(), confirmation.confirmedBy(),
                confirmation.confirmedAt(), confirmation.acknowledgedDefaultIdsJson(), parseId);
    }

    int revise(String parseId, int expectedRevision, int nextRevision, String status,
               String dialogueState, String intentJson, String correctionsJson,
               String inputHash, String contextHash, String previewHash,
               String resultHash, String requiredDefaultIds, String questionJson) {
        return jdbcTemplate.update("""
                UPDATE biz_process_ai_parse
                SET parse_revision = ?, status = ?, dialogue_state = ?, result_kind = 'EXTRACTION',
                    workflow_version = 2, intent_json = CAST(? AS JSON), understanding_json = NULL,
                    question_json = CAST(? AS JSON), corrections_json = CAST(? AS JSON), preview_hash = ?,
                    input_hash = ?, context_hash = ?, result_hash = ?,
                    required_default_ids = CAST(? AS JSON),
                    acknowledged_default_ids = NULL
                WHERE parse_id = ? AND parse_revision = ? AND status <> 'CONFIRMED'
                """, nextRevision, status, dialogueState, intentJson, questionJson, correctionsJson,
                previewHash, inputHash, contextHash, resultHash, requiredDefaultIds,
                parseId, expectedRevision);
    }

    private ProcessAiParseRecord map(ResultSet resultSet) throws SQLException {
        return new ProcessAiParseRecord(
                resultSet.getString("uuid"), resultSet.getString("order_uuid"),
                resultSet.getString("conversation_id"), resultSet.getString("parse_id"),
                resultSet.getInt("parse_revision"), resultSet.getInt("memory_generation"),
                resultSet.getString("request_idempotency_key"),
                resultSet.getInt("expected_version"), resultSet.getString("status"),
                resultSet.getString("provider"), resultSet.getString("model"),
                resultSet.getString("route"), resultSet.getString("schema_version"),
                resultSet.getString("project_memory_version"),
                resultSet.getString("project_memory_checksum"),
                resultSet.getString("project_memory_item_ids"), resultSet.getString("intent_json"),
                resultSet.getString("result_hash"), confirmation(resultSet),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getString("dialogue_state"), resultSet.getString("result_kind"),
                resultSet.getInt("workflow_version"), resultSet.getString("understanding_json"),
                resultSet.getString("question_json"), resultSet.getString("corrections_json"),
                resultSet.getString("input_hash"), resultSet.getString("context_hash"),
                resultSet.getString("preview_hash"), resultSet.getString("failure_code"),
                resultSet.getString("failure_trace_id"), resultSet.getString("required_default_ids"),
                resultSet.getString("acknowledged_default_ids"));
    }

    private ProcessAiParseConfirmation confirmation(ResultSet resultSet) throws SQLException {
        return new ProcessAiParseConfirmation(
                resultSet.getString("apply_idempotency_key"),
                resultSet.getString("accepted_field_paths"), resultSet.getString("plan_hash"),
                (Integer) resultSet.getObject("next_version"),
                resultSet.getString("confirmed_result_json"), resultSet.getString("confirmed_by"),
                resultSet.getTimestamp("confirmed_at") == null
                        ? null : resultSet.getTimestamp("confirmed_at").toLocalDateTime(),
                resultSet.getString("acknowledged_default_ids"));
    }
}
