package com.paper.mes.ai.memory.candidate;

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
class ProjectMemoryCandidateRepository {

    private static final String COLUMNS = """
            uuid, memory_id, candidate_type, candidate_json, status,
            distinct_order_count, first_seen_at, last_seen_at, expires_at,
            reviewed_by, review_notes, reviewed_at
            """;
    private static final String BY_MEMORY_ID = "SELECT " + COLUMNS + """
            FROM biz_project_memory_candidate WHERE memory_id = ? FOR UPDATE
            """;
    private static final String BY_UUID = "SELECT " + COLUMNS + """
            FROM biz_project_memory_candidate WHERE uuid = ? FOR UPDATE
            """;
    private static final String LIST_ALL = "SELECT " + COLUMNS + """
            FROM biz_project_memory_candidate
            ORDER BY FIELD(status, 'READY', 'CONFLICT', 'CANDIDATE', 'ACTIVE',
                           'REJECTED', 'EXPIRED'), last_seen_at DESC
            LIMIT 500
            """;
    private static final String LIST_STATUS = "SELECT " + COLUMNS + """
            FROM biz_project_memory_candidate WHERE status = ?
            ORDER BY last_seen_at DESC LIMIT 500
            """;

    private final JdbcTemplate jdbcTemplate;

    Optional<ProjectMemoryCandidateRow> findByMemoryIdForUpdate(String memoryId) {
        return one(jdbcTemplate.query(BY_MEMORY_ID, this::map, memoryId));
    }

    Optional<ProjectMemoryCandidateRow> findByUuidForUpdate(String uuid) {
        return one(jdbcTemplate.query(BY_UUID, this::map, uuid));
    }

    List<ProjectMemoryCandidateRow> list(String status) {
        return status == null ? jdbcTemplate.query(LIST_ALL, this::map)
                : jdbcTemplate.query(LIST_STATUS, this::map, status);
    }

    int insert(ProjectMemoryCandidateRow row) {
        return jdbcTemplate.update("""
                INSERT INTO biz_project_memory_candidate
                  (uuid, memory_id, candidate_type, candidate_json, status,
                   distinct_order_count, first_seen_at, last_seen_at, expires_at)
                VALUES (?, ?, ?, CAST(? AS JSON), ?, ?, ?, ?, ?)
                """, row.uuid(), row.memoryId(), row.candidateType(), row.candidateJson(),
                row.status(), row.distinctOrderCount(), row.firstSeenAt(),
                row.lastSeenAt(), row.expiresAt());
    }

    int insertEvidence(String uuid, String candidateUuid,
                       ProjectMemoryCandidateEvidenceWrite evidence) {
        return jdbcTemplate.update("""
                INSERT IGNORE INTO biz_project_memory_candidate_evidence
                  (uuid, candidate_uuid, order_uuid, parse_id, evidence_hash,
                   order_ref_hash, parse_ref_hash, audit_context_ciphertext, audit_context_hash,
                   source_type, phrase, context_json, proposed_value_json,
                   final_value_json, difference_json, preview_ready, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON),
                        CAST(? AS JSON), ?, ?)
                """, uuid, candidateUuid, evidence.orderUuid(), evidence.parseId(),
                evidence.evidenceHash(), evidence.orderRefHash(), evidence.parseRefHash(),
                evidence.auditContextCiphertext(), evidence.auditContextHash(),
                evidence.sourceType(), evidence.phrase(),
                evidence.contextJson(), evidence.proposedValueJson(),
                evidence.finalValueJson(), evidence.differenceJson(),
                evidence.previewReady(), evidence.createdBy());
    }

    List<ProjectMemoryCandidateEvidenceRow> listEvidence(String candidateUuid) {
        return jdbcTemplate.query("""
                SELECT evidence.uuid, evidence.candidate_uuid, evidence.source_type,
                       evidence.proposed_value_json, evidence.final_value_json, evidence.difference_json,
                       evidence.preview_ready, evidence.created_by, evidence.created_at
                FROM biz_project_memory_candidate_evidence evidence
                WHERE evidence.candidate_uuid = ?
                  AND evidence.audit_context_hash IS NOT NULL
                  AND evidence.context_json IS NULL
                  AND evidence.phrase IS NULL
                ORDER BY evidence.created_at DESC
                LIMIT 100
                """, this::mapEvidence, candidateUuid);
    }

    boolean evidenceReferencesReady() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM biz_project_memory_candidate_evidence
                WHERE (order_uuid IS NOT NULL AND order_ref_hash IS NULL)
                   OR (parse_id IS NOT NULL AND parse_ref_hash IS NULL)
                   OR phrase IS NOT NULL
                   OR context_json IS NOT NULL
                   OR (audit_context_hash IS NULL AND
                       (audit_context_ciphertext IS NOT NULL
                        OR proposed_value_json IS NOT NULL
                        OR final_value_json IS NOT NULL
                        OR difference_json IS NOT NULL))
                """, Integer.class);
        return count != null && count == 0;
    }

    int countRecentOrders(String candidateUuid, LocalDateTime since) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT COALESCE(order_ref_hash, order_uuid))
                FROM biz_project_memory_candidate_evidence
                WHERE candidate_uuid = ? AND created_at >= ?
                """, Integer.class, candidateUuid, since);
        return count == null ? 0 : count;
    }

    List<LegacyEvidenceReference> findLegacyReferences(int limit) {
        return jdbcTemplate.query("""
                SELECT uuid, candidate_uuid, order_uuid, parse_id
                FROM biz_project_memory_candidate_evidence
                WHERE (order_uuid IS NOT NULL AND order_ref_hash IS NULL)
                   OR (parse_id IS NOT NULL AND parse_ref_hash IS NULL)
                ORDER BY created_at, uuid
                LIMIT ?
                """, (resultSet, rowNumber) -> new LegacyEvidenceReference(
                resultSet.getString("uuid"), resultSet.getString("candidate_uuid"),
                resultSet.getString("order_uuid"), resultSet.getString("parse_id")), limit);
    }

    int backfillReference(String uuid, String orderRefHash, String parseRefHash) {
        return jdbcTemplate.update("""
                UPDATE biz_project_memory_candidate_evidence
                SET order_ref_hash = ?, parse_ref_hash = ?, order_uuid = NULL, parse_id = NULL
                WHERE uuid = ?
                  AND ((order_uuid IS NOT NULL AND order_ref_hash IS NULL)
                    OR (parse_id IS NOT NULL AND parse_ref_hash IS NULL))
                """, orderRefHash, parseRefHash, uuid);
    }

    List<LegacyEvidenceAuditContext> findLegacyAuditContexts(int limit) {
        return jdbcTemplate.query("""
                SELECT uuid, phrase, context_json, proposed_value_json,
                       final_value_json, difference_json
                FROM biz_project_memory_candidate_evidence
                WHERE audit_context_hash IS NULL
                  AND (phrase IS NOT NULL OR context_json IS NOT NULL
                   OR proposed_value_json IS NOT NULL
                   OR final_value_json IS NOT NULL
                   OR difference_json IS NOT NULL
                   OR (audit_context_ciphertext IS NULL
                       AND phrase IS NULL AND context_json IS NULL
                       AND proposed_value_json IS NULL
                       AND final_value_json IS NULL
                       AND difference_json IS NULL))
                ORDER BY created_at, uuid
                LIMIT ?
                """, (resultSet, rowNumber) -> new LegacyEvidenceAuditContext(
                resultSet.getString("uuid"), resultSet.getString("phrase"),
                resultSet.getString("context_json"),
                resultSet.getString("proposed_value_json"),
                resultSet.getString("final_value_json"),
                resultSet.getString("difference_json")), limit);
    }

    int backfillAuditContext(String uuid, String ciphertext, String hash) {
        return jdbcTemplate.update("""
                UPDATE biz_project_memory_candidate_evidence
                SET audit_context_ciphertext = ?, audit_context_hash = COALESCE(audit_context_hash, ?),
                    context_json = NULL, phrase = NULL, proposed_value_json = NULL,
                    final_value_json = NULL, difference_json = NULL
                WHERE uuid = ? AND audit_context_hash IS NULL
                  AND (phrase IS NOT NULL OR context_json IS NOT NULL
                   OR proposed_value_json IS NOT NULL
                   OR final_value_json IS NOT NULL
                   OR difference_json IS NOT NULL
                   OR (audit_context_ciphertext IS NULL
                       AND phrase IS NULL AND context_json IS NULL
                       AND proposed_value_json IS NULL
                       AND final_value_json IS NULL
                       AND difference_json IS NULL))
                """, ciphertext, hash, uuid);
    }

    int purgeAuditContextBefore(LocalDateTime cutoff) {
        return jdbcTemplate.update("""
                UPDATE biz_project_memory_candidate_evidence
                SET audit_context_ciphertext = NULL
                WHERE audit_context_ciphertext IS NOT NULL AND created_at < ?
                """, cutoff);
    }

    int refreshDistinctOrderCounts() {
        return jdbcTemplate.update("""
                UPDATE biz_project_memory_candidate candidate
                LEFT JOIN (
                    SELECT candidate_uuid, COUNT(DISTINCT order_ref_hash) AS order_count
                    FROM biz_project_memory_candidate_evidence
                    WHERE order_ref_hash IS NOT NULL
                    GROUP BY candidate_uuid
                ) evidence ON evidence.candidate_uuid = candidate.uuid
                SET candidate.distinct_order_count = COALESCE(evidence.order_count, 0)
                WHERE candidate.status <> 'ACTIVE'
                """);
    }

    int updateObservation(String uuid, String status, int count,
                          LocalDateTime observedAt, LocalDateTime expiresAt) {
        return jdbcTemplate.update("""
                UPDATE biz_project_memory_candidate
                SET status = ?, distinct_order_count = ?, last_seen_at = ?, expires_at = ?
                WHERE uuid = ?
                """, status, count, observedAt, expiresAt, uuid);
    }

    int review(String uuid, String status, String reviewer, String notes,
               String candidateJson, LocalDateTime reviewedAt) {
        return jdbcTemplate.update("""
                UPDATE biz_project_memory_candidate
                SET status = ?, reviewed_by = ?, review_notes = ?,
                    candidate_json = CAST(? AS JSON), reviewed_at = ?
                WHERE uuid = ?
                """, status, reviewer, notes, candidateJson, reviewedAt, uuid);
    }

    int expireCandidates(LocalDateTime now) {
        return jdbcTemplate.update("""
                UPDATE biz_project_memory_candidate
                SET status = 'EXPIRED'
                WHERE status = 'CANDIDATE' AND expires_at < ?
                """, now);
    }

    private ProjectMemoryCandidateRow map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProjectMemoryCandidateRow(
                resultSet.getString("uuid"), resultSet.getString("memory_id"),
                resultSet.getString("candidate_type"), resultSet.getString("candidate_json"),
                resultSet.getString("status"), resultSet.getInt("distinct_order_count"),
                time(resultSet, "first_seen_at"), time(resultSet, "last_seen_at"),
                time(resultSet, "expires_at"), resultSet.getString("reviewed_by"),
                resultSet.getString("review_notes"), time(resultSet, "reviewed_at"));
    }

    private ProjectMemoryCandidateEvidenceRow mapEvidence(ResultSet resultSet, int rowNumber)
            throws SQLException {
        Object ready = resultSet.getObject("preview_ready");
        return new ProjectMemoryCandidateEvidenceRow(
                resultSet.getString("uuid"), resultSet.getString("candidate_uuid"),
                resultSet.getString("source_type"), resultSet.getString("proposed_value_json"),
                resultSet.getString("final_value_json"),
                resultSet.getString("difference_json"),
                ready == null ? null : resultSet.getBoolean("preview_ready"),
                resultSet.getString("created_by"), time(resultSet, "created_at"));
    }

    private LocalDateTime time(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Optional<ProjectMemoryCandidateRow> one(List<ProjectMemoryCandidateRow> rows) {
        if (rows.size() > 1) throw new IllegalStateException("multiple memory candidates matched");
        return rows.stream().findFirst();
    }
}

record LegacyEvidenceReference(
        String uuid,
        String candidateUuid,
        String orderUuid,
        String parseId) {
}

record LegacyEvidenceAuditContext(
        String uuid,
        String phrase,
        String contextJson,
        String proposedValueJson,
        String finalValueJson,
        String differenceJson) {

    LegacyEvidenceAuditContext(String uuid, String phrase, String contextJson) {
        this(uuid, phrase, contextJson, null, null, null);
    }
}
