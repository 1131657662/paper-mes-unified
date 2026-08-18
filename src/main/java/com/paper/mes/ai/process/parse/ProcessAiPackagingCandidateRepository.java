package com.paper.mes.ai.process.parse;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class ProcessAiPackagingCandidateRepository {

    private final JdbcTemplate jdbcTemplate;

    int insert(ProcessAiPackagingCandidateRow row) {
        return jdbcTemplate.update("""
                INSERT INTO biz_process_ai_packaging_candidate
                  (uuid, order_uuid, conversation_id, parse_id, parse_revision,
                   owner_roll_ref, original_uuid, status, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)
                """, row.uuid(), row.orderUuid(), row.conversationId(), row.parseId(),
                row.parseRevision(), row.ownerRollRef(), row.originalUuid(),
                row.createdBy(), row.createdAt());
    }

    List<ProcessAiPackagingCandidateRow> findPending(String orderUuid, String createdBy) {
        return jdbcTemplate.query("""
                SELECT c.uuid, c.order_uuid, c.conversation_id, c.parse_id,
                       c.parse_revision, c.owner_roll_ref, c.original_uuid, c.status,
                       c.created_by, p.confirmed_result_json, c.created_at
                FROM biz_process_ai_packaging_candidate c
                INNER JOIN biz_process_ai_parse p
                  ON p.parse_id = c.parse_id AND p.status = 'CONFIRMED'
                WHERE c.order_uuid = ? AND c.created_by = ? AND c.status = 'PENDING'
                ORDER BY c.created_at, c.uuid
                """, (resultSet, rowNumber) -> mapPending(resultSet), orderUuid, createdBy);
    }

    int resolve(String orderUuid, String parseId, String ownerRollRef,
                String createdBy, String status, String resolvedBy) {
        return jdbcTemplate.update("""
                UPDATE biz_process_ai_packaging_candidate
                SET status = ?, resolved_by = ?, resolved_at = ?
                WHERE order_uuid = ? AND parse_id = ? AND owner_roll_ref = ?
                  AND created_by = ? AND status = 'PENDING'
                """, status, resolvedBy, LocalDateTime.now(), orderUuid,
                parseId, ownerRollRef, createdBy);
    }

    Optional<String> findStatus(String orderUuid, String parseId,
                                String ownerRollRef, String createdBy) {
        List<String> rows = jdbcTemplate.queryForList("""
                SELECT status FROM biz_process_ai_packaging_candidate
                WHERE order_uuid = ? AND parse_id = ? AND owner_roll_ref = ? AND created_by = ?
                """, String.class, orderUuid, parseId, ownerRollRef, createdBy);
        return rows.stream().findFirst();
    }

    private ProcessAiPackagingCandidateRow mapPending(ResultSet resultSet) throws SQLException {
        return new ProcessAiPackagingCandidateRow(
                resultSet.getString("uuid"), resultSet.getString("order_uuid"),
                resultSet.getString("conversation_id"), resultSet.getString("parse_id"),
                resultSet.getInt("parse_revision"), resultSet.getString("owner_roll_ref"),
                resultSet.getString("original_uuid"), resultSet.getString("status"),
                resultSet.getString("created_by"),
                resultSet.getString("confirmed_result_json"),
                resultSet.getTimestamp("created_at").toLocalDateTime());
    }
}
