package com.paper.mes.report.materialization.service;

import com.paper.mes.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportMetricPublicationService {
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public int publish(ReportMetricPublicationCommand command) {
        lockAndValidate(command);
        int inserted = jdbcTemplate.update(PUBLISH_VALUES_SQL, command.segmentUuid(), command.generationUuid());
        int states = jdbcTemplate.update(PUBLISH_STATE_SQL, command.taskId(), command.generationUuid(),
                LocalDateTime.now(), command.segmentUuid(), command.generationUuid());
        if (states == 0) throw new BusinessException("物化分片没有可发布的指标值");
        completeSegment(command, inserted);
        jdbcTemplate.update("DELETE FROM rpt_metric_value_stage WHERE segment_uuid = ? AND generation_uuid = ?",
                command.segmentUuid(), command.generationUuid());
        return inserted;
    }

    private void lockAndValidate(ReportMetricPublicationCommand command) {
        Integer count = jdbcTemplate.queryForObject(LOCK_SEGMENT_SQL, Integer.class,
                command.segmentUuid(), command.workerId(), command.fencingToken(), LocalDateTime.now());
        if (count == null || count != 1) throw new BusinessException("物化分片租约已失效");
        Integer invalid = jdbcTemplate.queryForObject(VALIDATE_STAGE_SQL, Integer.class,
                command.segmentUuid(), command.generationUuid());
        if (invalid == null || invalid > 0) throw new BusinessException("暂存指标版本不属于任务发布包");
    }

    private void completeSegment(ReportMetricPublicationCommand command, int rowCount) {
        int rows = jdbcTemplate.update(COMPLETE_SEGMENT_SQL, rowCount, LocalDateTime.now(),
                command.segmentUuid(), command.workerId(), command.fencingToken());
        if (rows != 1) throw new BusinessException("物化分片发布发生并发冲突");
    }

    private static final String LOCK_SEGMENT_SQL = """
            SELECT COUNT(*) FROM rpt_metric_materialization_segment
            WHERE uuid = ? AND segment_status = 2 AND lease_owner = ?
              AND fencing_token = ? AND lease_until >= ? FOR UPDATE
            """;
    private static final String PUBLISH_VALUES_SQL = """
            INSERT IGNORE INTO rpt_metric_value
              (period_start, uuid, generation_uuid, metric_release_uuid, metric_uuid,
               metric_version_uuid, period_end, dimension_set_code, grain_type, entity_uuid,
               dimension_hash, dimension_json, metric_value, source_as_of, published_at)
            SELECT period_start, REPLACE(UUID(), '-', ''), generation_uuid, metric_release_uuid,
                   metric_uuid, metric_version_uuid, period_end, dimension_set_code, grain_type,
                   entity_uuid, dimension_hash, dimension_json, metric_value, source_as_of, CURRENT_TIMESTAMP
            FROM rpt_metric_value_stage WHERE segment_uuid = ? AND generation_uuid = ?
            """;
    private static final String VALIDATE_STAGE_SQL = """
            SELECT COUNT(*)
            FROM rpt_metric_value_stage s
            JOIN rpt_metric_materialization_segment seg ON seg.uuid = s.segment_uuid
            JOIN rpt_metric_materialization_job job ON job.uuid = seg.job_uuid
            LEFT JOIN rpt_metric_release_item item ON item.release_uuid = s.metric_release_uuid
              AND item.metric_uuid = s.metric_uuid AND item.metric_version_uuid = s.metric_version_uuid
            WHERE s.segment_uuid = ? AND s.generation_uuid = ?
              AND (s.metric_release_uuid <> job.metric_release_uuid OR item.uuid IS NULL)
            """;
    private static final String PUBLISH_STATE_SQL = """
            INSERT INTO rpt_metric_materialization_state
              (uuid, task_id, metric_release_uuid, metric_uuid, metric_version_uuid,
               period_start, period_end, dimension_set_code, materialization_status,
               retry_count, active_generation_uuid, source_as_of, materialized_at)
            SELECT REPLACE(UUID(), '-', ''), ?, metric_release_uuid, metric_uuid, metric_version_uuid,
                   period_start, period_end, dimension_set_code, 2, 0, ?, MAX(source_as_of), ?
            FROM rpt_metric_value_stage WHERE segment_uuid = ? AND generation_uuid = ?
            GROUP BY metric_release_uuid, metric_uuid, metric_version_uuid,
                     period_start, period_end, dimension_set_code
            ON DUPLICATE KEY UPDATE task_id = VALUES(task_id), materialization_status = 2,
              retry_count = retry_count + 1,
              active_generation_uuid = VALUES(active_generation_uuid), source_as_of = VALUES(source_as_of),
              materialized_at = VALUES(materialized_at), error_message = NULL
            """;
    private static final String COMPLETE_SEGMENT_SQL = """
            UPDATE rpt_metric_materialization_segment
            SET segment_status = 3, row_count = ?, completed_at = ?, lease_owner = NULL, lease_until = NULL
            WHERE uuid = ? AND segment_status = 2 AND lease_owner = ? AND fencing_token = ?
            """;
}
