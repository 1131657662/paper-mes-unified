package com.paper.mes.report.materialization.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportSnapshotCleanupService {
    private final JdbcTemplate jdbcTemplate;

    public int cleanup(int retiredRetentionDays, int batchSize) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime retiredCutoff = now.minusDays(retiredRetentionDays);
        return jdbcTemplate.update(CLEANUP_SQL, now, retiredCutoff, batchSize);
    }

    private static final String CLEANUP_SQL = """
            DELETE FROM rpt_report_snapshot
            WHERE uuid IN (
              SELECT uuid FROM (
                SELECT s.uuid
                FROM rpt_report_snapshot s
                JOIN rpt_metric_release r ON r.uuid = s.metric_release_uuid
                WHERE NOT EXISTS (
                  SELECT 1 FROM rpt_report_snapshot_reference ref WHERE ref.snapshot_uuid = s.uuid
                )
                  AND (s.expires_at <= ? OR (r.release_status = 3 AND r.retired_at <= ?))
                ORDER BY s.expires_at, s.uuid
                LIMIT ?
              ) candidates
            )
            """;
}
