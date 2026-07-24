package com.paper.mes.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportQuerySnapshotVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportQuerySnapshotStore {
    private static final TypeReference<Map<String, String>> VERSION_MAP = new TypeReference<>() { };
    private static final String INSERT_SQL = """
            INSERT INTO rpt_report_query_snapshot
              (uuid, owner_uuid, owner_role_code, permission_hash, scope_hash, metric_release_uuid,
               query_hash, idempotency_bucket, query_json, metric_version_json, data_as_of,
               source_watermark, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND_SQL = """
            SELECT uuid, owner_uuid, permission_hash, scope_hash, metric_release_uuid, query_hash,
                   query_json, metric_version_json, data_as_of, source_watermark, expires_at
            FROM rpt_report_query_snapshot
            WHERE uuid = ? AND snapshot_status = 1
            """;
    private static final String REUSABLE_SQL = """
            SELECT uuid, owner_uuid, permission_hash, scope_hash, metric_release_uuid, query_hash,
                   query_json, metric_version_json, data_as_of, source_watermark, expires_at
            FROM rpt_report_query_snapshot
            WHERE owner_uuid = ? AND permission_hash = ? AND query_hash = ?
              AND metric_release_uuid = ? AND idempotency_bucket = ? AND snapshot_status = 1
            ORDER BY create_time DESC
            LIMIT 1
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void insert(ReportQuerySnapshotRecord record, String roleCode, long idempotencyBucket) {
        ReportQuerySnapshotVO value = record.snapshot();
        jdbcTemplate.update(INSERT_SQL, value.querySnapshotUuid(), record.ownerUuid(), roleCode,
                record.permissionHash(), value.scopeHash(), value.metricReleaseUuid(), value.queryHash(),
                idempotencyBucket, json(record.query()), json(value.metricVersionMap()), value.dataAsOf(),
                value.sourceWatermark(), value.expiresAt());
    }

    public Optional<ReportQuerySnapshotRecord> find(String uuid) {
        List<ReportQuerySnapshotRecord> rows = jdbcTemplate.query(FIND_SQL, this::record, uuid);
        return rows.stream().findFirst();
    }

    public Optional<ReportQuerySnapshotRecord> findReusable(ReportQuerySnapshotLookup lookup) {
        List<ReportQuerySnapshotRecord> rows = jdbcTemplate.query(REUSABLE_SQL, this::record,
                lookup.ownerUuid(), lookup.permissionHash(), lookup.queryHash(),
                lookup.metricReleaseUuid(), lookup.idempotencyBucket());
        return rows.stream().findFirst();
    }

    public int cleanupExpired(java.time.LocalDateTime now, int batchSize) {
        return jdbcTemplate.update(CLEANUP_SQL, now, batchSize);
    }

    private ReportQuerySnapshotRecord record(java.sql.ResultSet rs, int rowNum)
            throws java.sql.SQLException {
        return new ReportQuerySnapshotRecord(rs.getString("owner_uuid"),
                rs.getString("permission_hash"), readQuery(rs.getString("query_json")), snapshot(rs));
    }

    private ReportQuerySnapshotVO snapshot(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ReportQuerySnapshotVO(rs.getString("uuid"), rs.getString("uuid"), rs.getString("query_hash"),
                rs.getString("metric_release_uuid"), readVersions(rs.getString("metric_version_json")),
                time(rs.getTimestamp("data_as_of")), time(rs.getTimestamp("source_watermark")),
                time(rs.getTimestamp("expires_at")), rs.getString("scope_hash"),
                "LIVE_DB_READ", "LIVE_ONLY", List.of(),
                Map.of("overview", "READY", "dimensions", "READY", "details", "READY"));
    }

    private ReportQuery readQuery(String value) {
        try {
            return objectMapper.readValue(value, ReportQuery.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("报表查询快照内容无法解析");
        }
    }

    private Map<String, String> readVersions(String value) {
        try {
            return objectMapper.readValue(value, VERSION_MAP);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("报表指标版本快照无法解析");
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("报表查询快照无法序列化");
        }
    }

    private java.time.LocalDateTime time(Timestamp value) {
        return value.toLocalDateTime();
    }

    private static final String CLEANUP_SQL = """
            DELETE FROM rpt_report_query_snapshot
            WHERE uuid IN (
              SELECT uuid FROM (
                SELECT uuid FROM rpt_report_query_snapshot
                WHERE expires_at <= ?
                ORDER BY expires_at, uuid
                LIMIT ?
              ) expired
            )
            """;
}
