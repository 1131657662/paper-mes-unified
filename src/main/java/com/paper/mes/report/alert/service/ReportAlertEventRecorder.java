package com.paper.mes.report.alert.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportAlertEventRecorder {
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public RecordResult record(AlertEvent event) {
        String dimensionHash = ReportAlertEventKey.dimensionHash(event.canonicalDimensions());
        String eventKey = ReportAlertEventKey.generate(event.ruleUuid(), event.releaseUuid(),
                event.periodStart(), event.periodEnd(), dimensionHash);
        EventState previous = findForUpdate(eventKey);
        String eventUuid = previous == null ? stableEventUuid(eventKey) : previous.uuid();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(UPSERT_SQL, eventUuid, event.ruleUuid(), event.releaseUuid(), eventKey,
                event.periodStart(), event.periodEnd(), dimensionHash, event.metricValue(),
                event.thresholdValue(), event.severity(), now, now);
        return new RecordResult(eventUuid, previous == null || previous.status() == 2);
    }

    public boolean resolve(AlertIdentity identity) {
        String dimensionHash = ReportAlertEventKey.dimensionHash(identity.canonicalDimensions());
        String eventKey = ReportAlertEventKey.generate(identity.ruleUuid(), identity.releaseUuid(),
                identity.periodStart(), identity.periodEnd(), dimensionHash);
        LocalDateTime now = LocalDateTime.now();
        return jdbcTemplate.update(RESOLVE_SQL, now, now, eventKey) > 0;
    }

    public int resolveExpired(LocalDate currentPeriodStart) {
        LocalDateTime now = LocalDateTime.now();
        return jdbcTemplate.update(RESOLVE_EXPIRED_SQL, now, now, currentPeriodStart);
    }

    public int resolveInactiveRules() {
        LocalDateTime now = LocalDateTime.now();
        return jdbcTemplate.update(RESOLVE_INACTIVE_RULES_SQL, now, now);
    }

    public int resolveSupersededRelease(String activeReleaseUuid) {
        LocalDateTime now = LocalDateTime.now();
        return jdbcTemplate.update(RESOLVE_SUPERSEDED_RELEASE_SQL, now, now, activeReleaseUuid);
    }

    public int resolveOutdatedScopes(String activeReleaseUuid, LocalDate periodStart) {
        LocalDateTime now = LocalDateTime.now();
        return jdbcTemplate.update(RESOLVE_OUTDATED_SCOPES_SQL,
                now, now, activeReleaseUuid, periodStart);
    }

    public boolean acknowledge(String eventUuid) {
        return jdbcTemplate.update(ACKNOWLEDGE_SQL, LocalDateTime.now(),
                currentUserUuid(), eventUuid) > 0;
    }

    public boolean ignore(String eventUuid, String reason) {
        return jdbcTemplate.update(IGNORE_SQL, LocalDateTime.now(), currentUserUuid(), reason, eventUuid) > 0;
    }

    private EventState findForUpdate(String eventKey) {
        return jdbcTemplate.query(EVENT_STATE_SQL,
                (rs, rowNum) -> new EventState(rs.getString("uuid"), rs.getInt("event_status")), eventKey)
                .stream().findFirst().orElse(null);
    }

    private String stableEventUuid(String eventKey) {
        return eventKey.substring(0, 32);
    }

    public record AlertEvent(
            String ruleUuid,
            String releaseUuid,
            LocalDate periodStart,
            LocalDate periodEnd,
            String canonicalDimensions,
            BigDecimal metricValue,
            BigDecimal thresholdValue,
            int severity
    ) {
    }

    public record AlertIdentity(
            String ruleUuid,
            String releaseUuid,
            LocalDate periodStart,
            LocalDate periodEnd,
            String canonicalDimensions
    ) {
    }

    public record RecordResult(String eventUuid, boolean opened) {
    }

    private String currentUserUuid() {
        var user = com.paper.mes.auth.context.AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null || user.getUuid().isBlank()) {
            throw new com.paper.mes.common.BusinessException("当前登录身份无效");
        }
        return user.getUuid();
    }

    private record EventState(String uuid, int status) {
    }

    private static final String UPSERT_SQL = """
            INSERT INTO rpt_alert_event
              (uuid, rule_uuid, metric_release_uuid, event_key, period_start, period_end,
               dimension_hash, metric_value, threshold_value, severity, event_status,
               occurrence_count, first_detected_at, last_detected_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?, ?)
            ON DUPLICATE KEY UPDATE metric_value = VALUES(metric_value),
              threshold_value = VALUES(threshold_value), severity = VALUES(severity),
              event_status = IF(event_status = 3, 3, 1),
              resolved_at = IF(event_status = 3, resolved_at, NULL),
              acknowledged_at = IF(event_status = 3, acknowledged_at, NULL),
              acknowledged_by = IF(event_status = 3, acknowledged_by, NULL),
              ignored_at = IF(event_status = 3, ignored_at, NULL),
              ignored_by = IF(event_status = 3, ignored_by, NULL),
              ignore_reason = IF(event_status = 3, ignore_reason, NULL),
              occurrence_count = occurrence_count + 1,
              last_detected_at = VALUES(last_detected_at)
            """;
    private static final String EVENT_STATE_SQL = """
            SELECT uuid, event_status FROM rpt_alert_event
            WHERE event_key = ? AND is_deleted = 0
            FOR UPDATE
            """;
    private static final String RESOLVE_SQL = """
            UPDATE rpt_alert_event SET event_status = 2, resolved_at = ?, last_detected_at = ?
            WHERE event_key = ? AND is_deleted = 0 AND event_status IN (1, 3)
            """;
    private static final String ACKNOWLEDGE_SQL = """
            UPDATE rpt_alert_event SET acknowledged_at = ?, acknowledged_by = ?
            WHERE uuid = ? AND is_deleted = 0 AND event_status = 1 AND acknowledged_at IS NULL
            """;
    private static final String IGNORE_SQL = """
            UPDATE rpt_alert_event SET event_status = 3, ignored_at = ?, ignored_by = ?, ignore_reason = ?
            WHERE uuid = ? AND is_deleted = 0 AND event_status = 1
            """;
    private static final String RESOLVE_EXPIRED_SQL = """
            UPDATE rpt_alert_event SET event_status = 2, resolved_at = ?, last_detected_at = ?
            WHERE is_deleted = 0 AND event_status IN (1, 3) AND period_end < ?
            """;
    private static final String RESOLVE_INACTIVE_RULES_SQL = """
            UPDATE rpt_alert_event e
            JOIN rpt_alert_rule r ON r.uuid = e.rule_uuid
            SET e.event_status = 2, e.resolved_at = ?, e.last_detected_at = ?
            WHERE e.is_deleted = 0 AND e.event_status IN (1, 3)
              AND (r.is_deleted = 1 OR r.is_enabled = 0)
            """;
    private static final String RESOLVE_SUPERSEDED_RELEASE_SQL = """
            UPDATE rpt_alert_event SET event_status = 2, resolved_at = ?, last_detected_at = ?
            WHERE is_deleted = 0 AND event_status IN (1, 3) AND metric_release_uuid <> ?
            """;
    private static final String RESOLVE_OUTDATED_SCOPES_SQL = """
            UPDATE rpt_alert_event e
            JOIN rpt_alert_rule r ON r.uuid = e.rule_uuid
            SET e.event_status = 2, e.resolved_at = ?, e.last_detected_at = ?
            WHERE e.is_deleted = 0 AND e.event_status IN (1, 3)
              AND e.metric_release_uuid = ? AND e.period_start = ?
              AND e.dimension_hash <> SHA2(CASE r.scope_type
                WHEN 2 THEN CONCAT('CUSTOMER:', r.customer_uuid)
                WHEN 3 THEN CONCAT('PAPER:', r.paper_uuid)
                WHEN 4 THEN CONCAT('PROCESS:', r.process_type)
                ELSE 'GLOBAL' END, 256)
            """;
}
