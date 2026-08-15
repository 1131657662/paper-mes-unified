package com.paper.mes.ai.memory;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Reads and writes project-memory snapshots through the existing application datasource. */
@Repository
@RequiredArgsConstructor
public class ProjectMemoryDocumentRepository {

    private static final String ACTIVE_SQL = """
            SELECT uuid, doc_version, schema_version, checksum, doc_json, status,
                   patch_notes, created_by, approved_by
            FROM biz_project_memory_doc
            WHERE status = 'ACTIVE'
            ORDER BY created_at DESC
            """;

    private static final String INSERT_SQL = """
            INSERT INTO biz_project_memory_doc
              (uuid, doc_version, schema_version, checksum, doc_json, status,
               patch_notes, created_by, approved_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String ACTIVE_FOR_UPDATE_SQL = ACTIVE_SQL + " FOR UPDATE";
    private static final String VERSION_SQL = """
            SELECT uuid, doc_version, schema_version, checksum, doc_json, status,
                   patch_notes, created_by, approved_by
            FROM biz_project_memory_doc
            WHERE doc_version = ?
            FOR UPDATE
            """;
    private static final String VERSION_LIST_SQL = """
            SELECT doc_version, schema_version, checksum, status, patch_notes,
                   created_by, approved_by, created_at
            FROM biz_project_memory_doc
            ORDER BY created_at DESC,
                     CAST(SUBSTRING_INDEX(doc_version, '.', 1) AS UNSIGNED) DESC,
                     CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(doc_version, '.', 2), '.', -1) AS UNSIGNED) DESC,
                     CAST(SUBSTRING_INDEX(doc_version, '.', -1) AS UNSIGNED) DESC
            """;
    private static final String AUDIT_BY_KEY_SQL = """
            SELECT uuid, idempotency_key, operation_type, expected_memory_version,
                   old_doc_version, new_doc_version, old_checksum, new_checksum,
                   operations_json, reason, operator
            FROM biz_project_memory_patch_audit
            WHERE idempotency_key = ?
            """;
    private static final String AUDIT_INSERT_SQL = """
            INSERT INTO biz_project_memory_patch_audit
              (uuid, idempotency_key, operation_type, expected_memory_version,
               old_doc_version, new_doc_version, old_checksum, new_checksum,
               operations_json, reason, operator)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public Optional<ProjectMemoryDocumentRow> findActive() {
        List<ProjectMemoryDocumentRow> rows = jdbcTemplate.query(ACTIVE_SQL,
                (resultSet, rowNumber) -> map(resultSet));
        if (rows.size() > 1) {
            throw new IllegalStateException("multiple ACTIVE project memory snapshots found");
        }
        return rows.stream().findFirst();
    }

    public Optional<ProjectMemoryDocumentRow> findActiveForUpdate() {
        return one(jdbcTemplate.query(ACTIVE_FOR_UPDATE_SQL,
                (resultSet, rowNumber) -> map(resultSet)));
    }

    public Optional<ProjectMemoryDocumentRow> findVersionForUpdate(String version) {
        return one(jdbcTemplate.query(VERSION_SQL,
                (resultSet, rowNumber) -> map(resultSet), version));
    }

    public List<ProjectMemoryVersionRow> findVersions() {
        return jdbcTemplate.query(VERSION_LIST_SQL, (resultSet, rowNumber) -> mapVersion(resultSet));
    }

    public boolean existsVersion(String version) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_project_memory_doc WHERE doc_version = ?", Integer.class, version);
        return count != null && count > 0;
    }

    public Optional<ProjectMemoryPatchAuditRow> findAuditByIdempotencyKey(String key) {
        List<ProjectMemoryPatchAuditRow> rows = jdbcTemplate.query(AUDIT_BY_KEY_SQL,
                (resultSet, rowNumber) -> new ProjectMemoryPatchAuditRow(
                        resultSet.getString("uuid"), resultSet.getString("idempotency_key"),
                        resultSet.getString("operation_type"), resultSet.getString("expected_memory_version"),
                        resultSet.getString("old_doc_version"), resultSet.getString("new_doc_version"),
                        resultSet.getString("old_checksum"), resultSet.getString("new_checksum"),
                        resultSet.getString("operations_json"), resultSet.getString("reason"),
                        resultSet.getString("operator")), key);
        return one(rows);
    }

    public boolean hasAnyDocuments() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_project_memory_doc", Integer.class);
        return count != null && count > 0;
    }

    public int insert(ProjectMemoryDocumentRow row) {
        return jdbcTemplate.update(INSERT_SQL,
                row.uuid(), row.docVersion(), row.schemaVersion(), row.checksum(),
                row.docJson(), row.status(), row.patchNotes(), row.createdBy(), row.approvedBy());
    }

    public int markSuperseded(String version) {
        return jdbcTemplate.update("UPDATE biz_project_memory_doc SET status = 'SUPERSEDED' "
                + "WHERE doc_version = ? AND status = 'ACTIVE'", version);
    }

    public int markActive(String version) {
        return jdbcTemplate.update("UPDATE biz_project_memory_doc SET status = 'ACTIVE' "
                + "WHERE doc_version = ? AND status <> 'ACTIVE'", version);
    }

    public int insertAudit(ProjectMemoryPatchAuditRow row) {
        return jdbcTemplate.update(AUDIT_INSERT_SQL, row.uuid(), row.idempotencyKey(), row.operationType(),
                row.expectedMemoryVersion(), row.oldDocVersion(), row.newDocVersion(), row.oldChecksum(),
                row.newChecksum(), row.operationsJson(), row.reason(), row.operator());
    }

    private ProjectMemoryDocumentRow map(ResultSet resultSet) throws SQLException {
        return new ProjectMemoryDocumentRow(
                resultSet.getString("uuid"),
                resultSet.getString("doc_version"),
                resultSet.getString("schema_version"),
                resultSet.getString("checksum"),
                resultSet.getString("doc_json"),
                resultSet.getString("status"),
                resultSet.getString("patch_notes"),
                resultSet.getString("created_by"),
                resultSet.getString("approved_by"));
    }

    private ProjectMemoryVersionRow mapVersion(ResultSet resultSet) throws SQLException {
        return new ProjectMemoryVersionRow(
                resultSet.getString("doc_version"),
                resultSet.getString("schema_version"),
                resultSet.getString("checksum"),
                resultSet.getString("status"),
                resultSet.getString("patch_notes"),
                resultSet.getString("created_by"),
                resultSet.getString("approved_by"),
                resultSet.getTimestamp("created_at").toLocalDateTime());
    }

    private <T> Optional<T> one(List<T> rows) {
        if (rows.size() > 1) {
            throw new IllegalStateException("multiple project memory rows matched");
        }
        return rows.stream().findFirst();
    }
}
