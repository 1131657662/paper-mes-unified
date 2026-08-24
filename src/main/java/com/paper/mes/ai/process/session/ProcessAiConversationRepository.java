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
class ProcessAiConversationRepository {

    private static final String BY_ORDER_FOR_UPDATE_SQL = """
            SELECT uuid, conversation_id, order_uuid, user_uuid, current_step,
                   draft_version, project_memory_version, memory_generation, status,
                   clarification_round
            FROM biz_process_ai_conversation
            WHERE order_uuid = ?
            FOR UPDATE
            """;

    private static final String BY_ORDER_SQL = """
            SELECT uuid, conversation_id, order_uuid, user_uuid, current_step,
                   draft_version, project_memory_version, memory_generation, status,
                   clarification_round
            FROM biz_process_ai_conversation
            WHERE order_uuid = ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO biz_process_ai_conversation
              (uuid, conversation_id, order_uuid, user_uuid, current_step,
                draft_version, project_memory_version, memory_generation, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OPEN')
            """;

    private final JdbcTemplate jdbcTemplate;

    Optional<ProcessAiConversationRow> findByOrderForUpdate(String orderUuid) {
        List<ProcessAiConversationRow> rows = jdbcTemplate.query(BY_ORDER_FOR_UPDATE_SQL,
                (resultSet, rowNumber) -> map(resultSet), orderUuid);
        if (rows.size() > 1) {
            throw new IllegalStateException("multiple AI conversations found for one order");
        }
        return rows.stream().findFirst();
    }

    Optional<ProcessAiConversationRow> findByOrder(String orderUuid) {
        List<ProcessAiConversationRow> rows = jdbcTemplate.query(BY_ORDER_SQL,
                (resultSet, rowNumber) -> map(resultSet), orderUuid);
        if (rows.size() > 1) {
            throw new IllegalStateException("multiple AI conversations found for one order");
        }
        return rows.stream().findFirst();
    }

    List<String> findTerminalOrderUuids(int limit) {
        return jdbcTemplate.queryForList("""
                SELECT conversation.order_uuid
                FROM biz_process_ai_conversation conversation
                INNER JOIN biz_process_order process_order
                  ON process_order.uuid = conversation.order_uuid
                WHERE conversation.status IN ('OPEN', 'INTERRUPTED', 'EXPIRED')
                  AND process_order.order_status <> 0
                ORDER BY conversation.updated_at
                LIMIT ?
                """, String.class, limit);
    }

    int insert(ProcessAiConversationRow row) {
        return jdbcTemplate.update(INSERT_SQL, row.uuid(), row.conversationId(), row.orderUuid(),
                row.userUuid(), row.currentStep(), row.draftVersion(), row.projectMemoryVersion(),
                row.memoryGeneration());
    }

    int reopen(String conversationId, int currentStep) {
        return jdbcTemplate.update("""
                UPDATE biz_process_ai_conversation
                SET current_step = ?, status = 'OPEN'
                WHERE conversation_id = ? AND status IN ('OPEN', 'INTERRUPTED')
                """, currentStep, conversationId);
    }

    int close(String conversationId) {
        return jdbcTemplate.update("""
                UPDATE biz_process_ai_conversation
                SET status = 'CLOSED', closed_at = CURRENT_TIMESTAMP
                WHERE conversation_id = ? AND status <> 'CLOSED'
                """, conversationId);
    }

    int markInterrupted(String conversationId) {
        return jdbcTemplate.update("""
                UPDATE biz_process_ai_conversation
                SET status = 'INTERRUPTED'
                WHERE conversation_id = ? AND status = 'OPEN'
                """, conversationId);
    }

    int advanceDraftVersion(String conversationId, int expectedVersion, int nextVersion) {
        return jdbcTemplate.update("""
                UPDATE biz_process_ai_conversation
                SET draft_version = ?, status = 'OPEN'
                WHERE conversation_id = ? AND draft_version = ?
                  AND status IN ('OPEN', 'INTERRUPTED')
                """, nextVersion, conversationId, expectedVersion);
    }

    int reserveNextRevision(String conversationId) {
        int updated = jdbcTemplate.update("""
                UPDATE biz_process_ai_conversation
                SET last_parse_revision = last_parse_revision + 1, status = 'OPEN'
                WHERE conversation_id = ? AND status IN ('OPEN', 'INTERRUPTED')
                """, conversationId);
        if (updated != 1) return 0;
        Integer revision = jdbcTemplate.queryForObject("""
                SELECT last_parse_revision FROM biz_process_ai_conversation
                WHERE conversation_id = ?
                """, Integer.class, conversationId);
        return revision == null ? 0 : revision;
    }

    int reserveNextRevision(String conversationId, String action) {
        int updated = jdbcTemplate.update("""
                UPDATE biz_process_ai_conversation
                SET last_parse_revision = last_parse_revision + 1,
                    clarification_round = clarification_round +
                        CASE WHEN ? = 'CLARIFY' THEN 1 ELSE 0 END,
                    status = 'OPEN'
                WHERE conversation_id = ? AND status IN ('OPEN', 'INTERRUPTED')
                """, action, conversationId);
        if (updated != 1) return 0;
        Integer revision = jdbcTemplate.queryForObject("""
                SELECT last_parse_revision FROM biz_process_ai_conversation
                WHERE conversation_id = ?
                """, Integer.class, conversationId);
        return revision == null ? 0 : revision;
    }

    int refreshMemory(String conversationId, int expectedGeneration, String memoryVersion) {
        return jdbcTemplate.update("""
                UPDATE biz_process_ai_conversation
                SET project_memory_version = ?, memory_generation = memory_generation + 1,
                    status = 'OPEN'
                WHERE conversation_id = ? AND memory_generation = ?
                  AND status IN ('OPEN', 'INTERRUPTED')
                """, memoryVersion, conversationId, expectedGeneration);
    }

    private ProcessAiConversationRow map(ResultSet resultSet) throws SQLException {
        return new ProcessAiConversationRow(
                resultSet.getString("uuid"), resultSet.getString("conversation_id"),
                resultSet.getString("order_uuid"), resultSet.getString("user_uuid"),
                resultSet.getInt("current_step"), resultSet.getInt("draft_version"),
                resultSet.getString("project_memory_version"),
                resultSet.getInt("memory_generation"), resultSet.getString("status"),
                resultSet.getInt("clarification_round"));
    }
}
