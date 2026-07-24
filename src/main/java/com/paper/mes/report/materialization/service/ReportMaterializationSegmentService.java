package com.paper.mes.report.materialization.service;

import lombok.RequiredArgsConstructor;
import com.paper.mes.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportMaterializationSegmentService {
    private final JdbcTemplate jdbcTemplate;

    public String createOrGet(String jobUuid, String segmentKey, LocalDate start, LocalDate end) {
        String uuid = compactUuid();
        jdbcTemplate.update(INSERT_SQL, uuid, jobUuid, segmentKey, start, end);
        return jdbcTemplate.queryForObject("SELECT uuid FROM rpt_metric_materialization_segment "
                + "WHERE job_uuid = ? AND segment_key = ?", String.class, jobUuid, segmentKey);
    }

    public Optional<ReportMaterializationSegmentLease> claimNext(
            String jobUuid, String workerId, Duration duration) {
        requireWorker(workerId, duration);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plus(duration);
        String claimOwner = claimOwner(workerId);
        int rows = jdbcTemplate.update(CLAIM_SQL, claimOwner, until, now, jobUuid, now);
        if (rows == 0) return Optional.empty();
        List<ReportMaterializationSegmentLease> leases = jdbcTemplate.query(LEASE_SQL,
                (rs, rowNum) -> new ReportMaterializationSegmentLease(rs.getString("uuid"),
                        rs.getString("lease_owner"), rs.getLong("fencing_token"),
                        rs.getTimestamp("lease_until").toLocalDateTime()), jobUuid, claimOwner);
        return leases.stream().findFirst();
    }

    public boolean heartbeat(ReportMaterializationSegmentLease lease, Duration duration) {
        return jdbcTemplate.update("UPDATE rpt_metric_materialization_segment SET lease_until = ? "
                        + "WHERE uuid = ? AND segment_status = 2 AND lease_owner = ? AND fencing_token = ?",
                LocalDateTime.now().plus(duration), lease.segmentUuid(), lease.leaseOwner(), lease.fencingToken()) == 1;
    }

    public boolean fail(ReportMaterializationSegmentLease lease, String errorMessage) {
        String error = errorMessage == null ? "物化分片失败"
                : errorMessage.substring(0, Math.min(1000, errorMessage.length()));
        return jdbcTemplate.update(FAIL_SQL, LocalDateTime.now(), error,
                lease.segmentUuid(), lease.leaseOwner(), lease.fencingToken()) == 1;
    }

    private String claimOwner(String workerId) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String prefix = workerId.substring(0, Math.min(90, workerId.length()));
        return prefix + ":" + suffix;
    }

    private void requireWorker(String workerId, Duration duration) {
        if (workerId == null || workerId.isBlank() || duration == null
                || duration.isZero() || duration.isNegative()) {
            throw new BusinessException("物化分片租约参数无效");
        }
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static final String INSERT_SQL = """
            INSERT IGNORE INTO rpt_metric_materialization_segment
              (uuid, job_uuid, segment_key, period_start, period_end)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String CLAIM_SQL = """
            UPDATE rpt_metric_materialization_segment
            SET retry_count = retry_count + IF(segment_status IN (2, 4), 1, 0),
                segment_status = 2, lease_owner = ?, lease_until = ?,
                fencing_token = fencing_token + 1, started_at = COALESCE(started_at, ?),
                completed_at = NULL, error_message = NULL
            WHERE uuid = (
              SELECT uuid FROM (
                SELECT uuid FROM rpt_metric_materialization_segment
                WHERE job_uuid = ? AND (segment_status IN (1, 4)
                  OR (segment_status = 2 AND lease_until < ?))
                ORDER BY period_start, segment_key LIMIT 1
              ) candidate
            )
            """;
    private static final String LEASE_SQL = """
            SELECT uuid, lease_owner, fencing_token, lease_until
            FROM rpt_metric_materialization_segment
            WHERE job_uuid = ? AND segment_status = 2 AND lease_owner = ?
            """;
    private static final String FAIL_SQL = """
            UPDATE rpt_metric_materialization_segment
            SET segment_status = 4, completed_at = ?, error_message = ?, lease_owner = NULL, lease_until = NULL
            WHERE uuid = ? AND segment_status = 2 AND lease_owner = ? AND fencing_token = ?
            """;
}
