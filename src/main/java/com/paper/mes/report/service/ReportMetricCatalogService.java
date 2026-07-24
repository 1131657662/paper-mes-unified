package com.paper.mes.report.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.report.dto.ReportMetricContextVO;
import com.paper.mes.report.dto.ReportMetricItemVO;
import com.paper.mes.report.dto.ReportMetricReleaseDetailVO;
import com.paper.mes.report.dto.ReportMetricReleaseSummaryVO;
import com.paper.mes.report.dto.ReportMetricVersionAuditVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportMetricCatalogService {

    private final JdbcTemplate jdbcTemplate;

    public ReportMetricContextVO activeContext() {
        List<CatalogRow> rows = jdbcTemplate.query(ACTIVE_RELEASE_SQL,
                (rs, rowNum) -> mapRow(rs));
        if (rows.isEmpty()) {
            throw new BusinessException(ResultCode.ERROR, "报表指标发布包未初始化");
        }
        CatalogRow release = rows.getFirst();
        List<ReportMetricItemVO> metrics = rows.stream().map(CatalogRow::metric).toList();
        return new ReportMetricContextVO(release.releaseUuid(), release.releaseCode(),
                release.releaseName(), release.releaseChecksum(), release.publishedAt(),
                release.asOf(), metrics);
    }

    public List<ReportMetricReleaseSummaryVO> releaseHistory() {
        return jdbcTemplate.query(RELEASE_HISTORY_SQL, (rs, rowNum) -> mapRelease(rs));
    }

    public ReportMetricReleaseDetailVO releaseDetail(String releaseUuid) {
        List<ReportMetricReleaseSummaryVO> releases = jdbcTemplate.query(RELEASE_DETAIL_SQL,
                (rs, rowNum) -> mapRelease(rs), releaseUuid);
        if (releases.isEmpty()) throw new BusinessException("指标发布包不存在");
        List<ReportMetricVersionAuditVO> metrics = jdbcTemplate.query(RELEASE_METRICS_SQL,
                (rs, rowNum) -> mapAuditMetric(rs), releaseUuid);
        return new ReportMetricReleaseDetailVO(releases.getFirst(), metrics);
    }

    private CatalogRow mapRow(ResultSet rs) throws SQLException {
        ReportMetricItemVO metric = new ReportMetricItemVO(
                rs.getString("metric_uuid"), rs.getString("metric_code"),
                rs.getString("metric_name"), rs.getString("description"),
                rs.getString("value_type"), rs.getString("unit_code"),
                rs.getInt("display_scale"), rs.getString("metric_version_uuid"),
                rs.getInt("version_no"), rs.getString("definition_checksum"));
        return new CatalogRow(rs.getString("release_uuid"), rs.getString("release_code"),
                rs.getString("release_name"), rs.getString("release_checksum"),
                timestamp(rs, "published_at"), timestamp(rs, "as_of"), metric);
    }

    private LocalDateTime timestamp(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private ReportMetricReleaseSummaryVO mapRelease(ResultSet rs) throws SQLException {
        return new ReportMetricReleaseSummaryVO(rs.getString("release_uuid"),
                rs.getString("release_code"), rs.getString("release_name"),
                rs.getInt("release_status"), rs.getString("release_checksum"),
                rs.getLong("metric_count"), timestamp(rs, "published_at"),
                rs.getString("published_by"), timestamp(rs, "retired_at"),
                rs.getString("retired_by"), timestamp(rs, "create_time"),
                timestamp(rs, "as_of"));
    }

    private ReportMetricVersionAuditVO mapAuditMetric(ResultSet rs) throws SQLException {
        return new ReportMetricVersionAuditVO(rs.getString("metric_uuid"),
                rs.getString("metric_code"), rs.getString("metric_name"),
                rs.getString("description"), rs.getString("value_type"),
                rs.getString("unit_code"), rs.getInt("display_scale"),
                rs.getInt("display_order"), rs.getString("metric_version_uuid"),
                rs.getInt("version_no"), rs.getString("implementation_key"),
                rs.getString("definition_json"), rs.getString("definition_checksum"),
                rs.getInt("version_status"), timestamp(rs, "locked_at"),
                rs.getString("locked_by"));
    }

    private record CatalogRow(
            String releaseUuid, String releaseCode, String releaseName,
            String releaseChecksum, LocalDateTime publishedAt, LocalDateTime asOf,
            ReportMetricItemVO metric) {
    }

    private static final String ACTIVE_RELEASE_SQL = """
            SELECT r.uuid AS release_uuid, r.release_code, r.release_name,
                   r.release_checksum, r.published_at, CURRENT_TIMESTAMP AS as_of,
                   d.uuid AS metric_uuid, d.metric_code, d.metric_name, d.description,
                   d.value_type, d.unit_code, d.display_scale,
                   v.uuid AS metric_version_uuid, v.version_no, v.definition_checksum
            FROM rpt_metric_release r
            JOIN rpt_metric_release_item i ON i.release_uuid = r.uuid
            JOIN rpt_metric_definition d ON d.uuid = i.metric_uuid
              AND d.is_deleted = 0 AND d.is_enabled = 1
            JOIN rpt_metric_version v ON v.uuid = i.metric_version_uuid
              AND v.metric_uuid = d.uuid AND v.is_deleted = 0 AND v.version_status = 2
            WHERE r.is_deleted = 0 AND r.release_status = 2
            ORDER BY i.display_order, d.metric_code
            """;

    private static final String RELEASE_SUMMARY_SELECT = """
            SELECT r.uuid AS release_uuid, r.release_code, r.release_name,
                   r.release_status, r.release_checksum, COALESCE(c.metric_count, 0) AS metric_count,
                   r.published_at, r.published_by, r.retired_at, r.retired_by,
                   r.create_time, CURRENT_TIMESTAMP AS as_of
            FROM rpt_metric_release r
            LEFT JOIN (
              SELECT release_uuid, COUNT(*) AS metric_count
              FROM rpt_metric_release_item GROUP BY release_uuid
            ) c ON c.release_uuid = r.uuid
            """;
    private static final String RELEASE_HISTORY_SQL = RELEASE_SUMMARY_SELECT + """
            WHERE r.is_deleted = 0
            ORDER BY CASE r.release_status WHEN 2 THEN 0 WHEN 1 THEN 1 ELSE 2 END,
                     COALESCE(r.published_at, r.create_time) DESC, r.uuid
            """;
    private static final String RELEASE_DETAIL_SQL = RELEASE_SUMMARY_SELECT + """
            WHERE r.uuid = ? AND r.is_deleted = 0
            """;
    private static final String RELEASE_METRICS_SQL = """
            SELECT d.uuid AS metric_uuid, d.metric_code, d.metric_name, d.description,
                   d.value_type, d.unit_code, d.display_scale, i.display_order,
                   v.uuid AS metric_version_uuid, v.version_no, v.implementation_key,
                   CAST(v.definition_json AS CHAR) AS definition_json,
                   v.definition_checksum, v.version_status, v.locked_at, v.locked_by
            FROM rpt_metric_release_item i
            JOIN rpt_metric_definition d ON d.uuid = i.metric_uuid
            JOIN rpt_metric_version v ON v.uuid = i.metric_version_uuid
              AND v.metric_uuid = i.metric_uuid
            WHERE i.release_uuid = ?
            ORDER BY i.display_order, d.metric_code
            """;
}
