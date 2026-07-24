package com.paper.mes.report.materialization.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.materialization.dto.ReportMaterializationJobRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportMaterializationJobService {
    private final JdbcTemplate jdbcTemplate;

    public String createOrGet(ReportMaterializationJobRequest request) {
        validate(request);
        String uuid = compactUuid();
        jdbcTemplate.update(INSERT_SQL, uuid, request.taskId(), request.metricReleaseUuid(),
                request.periodStart(), request.periodEnd(), request.requestedBy());
        return jdbcTemplate.queryForObject(
                "SELECT uuid FROM rpt_metric_materialization_job WHERE task_id = ?", String.class, request.taskId());
    }

    public Optional<ReportMaterializationLease> claim(String jobUuid, String workerId, Duration duration) {
        requireWorker(workerId, duration);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plus(duration);
        int rows = jdbcTemplate.update(CLAIM_SQL, workerId, until, now, jobUuid, now);
        if (rows == 0) return Optional.empty();
        List<ReportMaterializationLease> leases = jdbcTemplate.query(LEASE_SQL,
                (rs, rowNum) -> new ReportMaterializationLease(rs.getString("uuid"),
                        rs.getString("lease_owner"), rs.getLong("fencing_token"),
                        rs.getTimestamp("lease_until").toLocalDateTime()), jobUuid, workerId);
        return leases.stream().findFirst();
    }

    public boolean heartbeat(ReportMaterializationLease lease, Duration duration) {
        return jdbcTemplate.update("UPDATE rpt_metric_materialization_job SET lease_until = ? "
                        + "WHERE uuid = ? AND job_status = 2 AND lease_owner = ? AND fencing_token = ?",
                LocalDateTime.now().plus(duration), lease.jobUuid(), lease.workerId(), lease.fencingToken()) == 1;
    }

    public boolean complete(ReportMaterializationLease lease) {
        return finish(lease, 3, null);
    }

    public boolean fail(ReportMaterializationLease lease, String errorMessage) {
        String safeError = errorMessage == null ? "物化任务失败" : errorMessage.substring(0, Math.min(1000, errorMessage.length()));
        return finish(lease, 4, safeError);
    }

    private boolean finish(ReportMaterializationLease lease, int status, String error) {
        return jdbcTemplate.update(FINISH_SQL, status, LocalDateTime.now(), error,
                lease.jobUuid(), lease.workerId(), lease.fencingToken()) == 1;
    }

    private void validate(ReportMaterializationJobRequest request) {
        if (request.taskId() == null || request.taskId().isBlank() || request.taskId().length() > 64) {
            throw new BusinessException("物化任务请求号无效");
        }
        if (request.periodStart() == null || request.periodEnd() == null
                || request.periodStart().isAfter(request.periodEnd())) {
            throw new BusinessException("物化日期范围无效");
        }
    }

    private void requireWorker(String workerId, Duration duration) {
        if (workerId == null || workerId.isBlank() || workerId.length() > 100
                || duration == null || duration.isZero() || duration.isNegative()) {
            throw new BusinessException("物化任务租约参数无效");
        }
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static final String INSERT_SQL = """
            INSERT IGNORE INTO rpt_metric_materialization_job
              (uuid, task_id, metric_release_uuid, period_start, period_end, requested_by)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String CLAIM_SQL = """
            UPDATE rpt_metric_materialization_job
            SET retry_count = retry_count + IF(job_status IN (2, 4), 1, 0),
                job_status = 2, lease_owner = ?, lease_until = ?,
                fencing_token = fencing_token + 1, started_at = COALESCE(started_at, ?),
                completed_at = NULL, error_message = NULL
            WHERE uuid = ? AND (job_status IN (1, 4) OR (job_status = 2 AND lease_until < ?))
            """;
    private static final String LEASE_SQL = """
            SELECT uuid, lease_owner, fencing_token, lease_until
            FROM rpt_metric_materialization_job
            WHERE uuid = ? AND job_status = 2 AND lease_owner = ?
            """;
    private static final String FINISH_SQL = """
            UPDATE rpt_metric_materialization_job
            SET job_status = ?, completed_at = ?, error_message = ?, lease_owner = NULL, lease_until = NULL
            WHERE uuid = ? AND job_status = 2 AND lease_owner = ? AND fencing_token = ?
            """;
}
