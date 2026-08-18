package com.paper.mes.ai.memory.candidate;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
class ProjectMemoryLearningOutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    int enqueue(String uuid, String eventKey, String eventType, String payloadJson) {
        return jdbcTemplate.update("""
                INSERT IGNORE INTO biz_project_memory_learning_outbox
                  (uuid, event_key, event_type, payload_json, status,
                   attempt_count, next_attempt_at)
                VALUES (?, ?, ?, CAST(? AS JSON), 'PENDING', 0, CURRENT_TIMESTAMP)
                """, uuid, eventKey, eventType, payloadJson);
    }

    List<ProjectMemoryLearningOutboxRow> findDue(int limit) {
        return jdbcTemplate.query("""
                SELECT uuid, event_key, event_type, payload_json,
                       attempt_count, next_attempt_at
                FROM biz_project_memory_learning_outbox
                WHERE status IN ('PENDING', 'FAILED') AND next_attempt_at <= CURRENT_TIMESTAMP
                ORDER BY next_attempt_at, created_at, uuid
                LIMIT ?
                """, this::map, limit);
    }

    int requeueStaleProcessing(LocalDateTime staleBefore) {
        return jdbcTemplate.update("""
                UPDATE biz_project_memory_learning_outbox
                SET status = 'FAILED', attempt_count = attempt_count + 1,
                    last_error = 'Recovered stale PROCESSING event after worker interruption',
                    next_attempt_at = CURRENT_TIMESTAMP
                WHERE status = 'PROCESSING' AND updated_at < ?
                """, staleBefore);
    }

    int claim(String uuid) {
        return jdbcTemplate.update("""
                UPDATE biz_project_memory_learning_outbox
                SET status = 'PROCESSING'
                WHERE uuid = ? AND status IN ('PENDING', 'FAILED')
                """, uuid);
    }

    int complete(String uuid) {
        return jdbcTemplate.update("""
                UPDATE biz_project_memory_learning_outbox
                SET status = 'COMPLETED', last_error = NULL
                WHERE uuid = ? AND status = 'PROCESSING'
                """, uuid);
    }

    int fail(String uuid, int attemptCount, String error, LocalDateTime nextAttemptAt) {
        return jdbcTemplate.update("""
                UPDATE biz_project_memory_learning_outbox
                SET status = 'FAILED', attempt_count = ?, last_error = ?, next_attempt_at = ?
                WHERE uuid = ? AND status = 'PROCESSING'
                """, attemptCount, error, nextAttemptAt, uuid);
    }

    private ProjectMemoryLearningOutboxRow map(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new ProjectMemoryLearningOutboxRow(
                resultSet.getString("uuid"), resultSet.getString("event_key"),
                resultSet.getString("event_type"), resultSet.getString("payload_json"),
                resultSet.getInt("attempt_count"),
                resultSet.getTimestamp("next_attempt_at").toLocalDateTime());
    }
}
