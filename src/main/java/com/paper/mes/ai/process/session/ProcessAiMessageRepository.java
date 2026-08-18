package com.paper.mes.ai.process.session;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class ProcessAiMessageRepository {

    private static final String SELECT_COLUMNS = """
             SELECT uuid, conversation_id, memory_generation, sequence_no, role, message_status,
                   idempotency_key, content_ciphertext, content_hash,
                   structured_result, created_at
            FROM biz_process_ai_message
            """;

    private final JdbcTemplate jdbcTemplate;

    Optional<ProcessAiMessageRow> findByIdempotencyKey(String conversationId,
                                                       int generation, String key) {
        List<ProcessAiMessageRow> rows = jdbcTemplate.query(SELECT_COLUMNS + """
                WHERE conversation_id = ? AND memory_generation = ? AND idempotency_key = ?
                """, (resultSet, rowNumber) -> map(resultSet), conversationId, generation, key);
        return rows.stream().findFirst();
    }

    List<ProcessAiMessageRow> findByConversation(String conversationId, int generation) {
        return jdbcTemplate.query(SELECT_COLUMNS + """
                WHERE conversation_id = ? AND memory_generation = ?
                ORDER BY sequence_no
                """, (resultSet, rowNumber) -> map(resultSet), conversationId, generation);
    }

    Optional<ProcessAiMessageRow> findBySequence(String conversationId, int generation,
                                                 int sequenceNo) {
        List<ProcessAiMessageRow> rows = jdbcTemplate.query(SELECT_COLUMNS + """
                WHERE conversation_id = ? AND memory_generation = ? AND sequence_no = ?
                """, (resultSet, rowNumber) -> map(resultSet), conversationId,
                generation, sequenceNo);
        return rows.stream().findFirst();
    }

    int nextSequence(String conversationId) {
        Integer value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(sequence_no), 0) + 1
                FROM biz_process_ai_message
                WHERE conversation_id = ?
                """, Integer.class, conversationId);
        return value == null ? 1 : value;
    }

    int insert(ProcessAiMessageRow row) {
        return jdbcTemplate.update("""
                INSERT INTO biz_process_ai_message
                  (uuid, conversation_id, sequence_no, role, message_status,
                   memory_generation, idempotency_key, content_ciphertext, content_hash,
                   structured_result)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON))
                """, row.uuid(), row.conversationId(), row.sequenceNo(), row.role(),
                row.messageStatus(), row.memoryGeneration(), row.idempotencyKey(),
                row.contentCiphertext(), row.contentHash(), row.structuredResult());
    }

    int deleteByConversation(String conversationId) {
        return jdbcTemplate.update(
                "DELETE FROM biz_process_ai_message WHERE conversation_id = ?", conversationId);
    }

    int updateContent(ProcessAiMessageRow row) {
        return jdbcTemplate.update("""
                UPDATE biz_process_ai_message
                SET message_status = ?, content_ciphertext = ?, content_hash = ?,
                    structured_result = CAST(? AS JSON)
                WHERE conversation_id = ? AND sequence_no = ? AND role = 'ASSISTANT'
                  AND message_status = 'PARTIAL'
                """, row.messageStatus(), row.contentCiphertext(), row.contentHash(),
                row.structuredResult(), row.conversationId(), row.sequenceNo());
    }

    private ProcessAiMessageRow map(ResultSet resultSet) throws SQLException {
        return new ProcessAiMessageRow(
                resultSet.getString("uuid"), resultSet.getString("conversation_id"),
                resultSet.getInt("memory_generation"), resultSet.getInt("sequence_no"),
                resultSet.getString("role"),
                resultSet.getString("message_status"), resultSet.getString("idempotency_key"),
                resultSet.getString("content_ciphertext"), resultSet.getString("content_hash"),
                resultSet.getString("structured_result"),
                resultSet.getTimestamp("created_at").toLocalDateTime());
    }
}
