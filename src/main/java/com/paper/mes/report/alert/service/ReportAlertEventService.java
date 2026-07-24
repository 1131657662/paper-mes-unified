package com.paper.mes.report.alert.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.report.alert.dto.ReportAlertEventPageVO;
import com.paper.mes.report.alert.dto.ReportAlertEventQuery;
import com.paper.mes.report.alert.dto.ReportAlertEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportAlertEventService {
    private final JdbcTemplate jdbcTemplate;
    private final ReportAlertEventRecorder eventRecorder;
    private final OperationLogService operationLogService;

    public ReportAlertEventPageVO page(ReportAlertEventQuery query) {
        QueryParts parts = QueryParts.from(query, true, true);
        long total = jdbcTemplate.queryForObject(COUNT_SQL + parts.where(), Long.class, parts.args().toArray());
        long[] summary = summary(parts.query());
        List<Object> args = new ArrayList<>(parts.args());
        args.add(query.pageSize());
        args.add((query.pageNumber() - 1) * query.pageSize());
        List<ReportAlertEventVO> items = jdbcTemplate.query(LIST_SQL + parts.where() + ORDER_SQL,
                EVENT_ROW, args.toArray());
        return new ReportAlertEventPageVO(items, total, query.pageNumber(), query.pageSize(),
                summary[0], summary[1], summary[2]);
    }

    private long[] summary(ReportAlertEventQuery query) {
        QueryParts parts = QueryParts.from(query, false, false);
        List<Long> values = jdbcTemplate.queryForObject(SUMMARY_SQL + parts.where(),
                (rs, row) -> List.of(rs.getLong(1), rs.getLong(2), rs.getLong(3)), parts.args().toArray());
        return new long[]{values.get(0), values.get(1), values.get(2)};
    }

    @Transactional
    public void acknowledge(String uuid) {
        requireChanged(eventRecorder.acknowledge(uuid), "事件不存在、已恢复或已经确认");
        operationLogService.record(OperationLogService.BIZ_TYPE_REPORT, uuid, null,
                OperationLogService.ACTION_REPORT_ALERT_ACKNOWLEDGE, null, null);
    }

    @Transactional
    public void ignore(String uuid, String reason) {
        String normalized = reason == null ? "" : reason.trim();
        if (normalized.isEmpty() || normalized.length() > 500) {
            throw new BusinessException("忽略原因必须填写且不超过500个字符");
        }
        requireChanged(eventRecorder.ignore(uuid, normalized), "事件不存在、已恢复或已经忽略");
        operationLogService.record(OperationLogService.BIZ_TYPE_REPORT, uuid, null,
                OperationLogService.ACTION_REPORT_ALERT_IGNORE, null, normalized);
    }

    private void requireChanged(boolean changed, String message) {
        if (!changed) throw new BusinessException(message);
    }

    private record QueryParts(ReportAlertEventQuery query, String where, List<Object> args) {
        static QueryParts from(ReportAlertEventQuery query, boolean includeStatus, boolean includeFocus) {
            StringBuilder where = new StringBuilder(" WHERE e.is_deleted = 0");
            List<Object> args = new ArrayList<>();
            if (includeStatus && query.getStatus() != null && query.getFocusUuid() == null) {
                where.append(" AND e.event_status = ?");
                args.add(query.getStatus());
            }
            if (query.getSeverity() != null) {
                where.append(" AND e.severity = ?");
                args.add(query.getSeverity());
            }
            if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
                where.append(" AND (r.rule_name LIKE ? OR c.customer_name LIKE ? OR p.paper_name LIKE ?)");
                String keyword = "%" + query.getKeyword().trim() + "%";
                args.add(keyword);
                args.add(keyword);
                args.add(keyword);
            }
            if (includeFocus && query.getFocusUuid() != null) {
                where.append(" AND e.uuid = ?");
                args.add(query.getFocusUuid());
            }
            return new QueryParts(query, where.toString(), args);
        }
    }

    private static final String JOINS = "JOIN rpt_alert_rule r ON r.uuid = e.rule_uuid "
            + "LEFT JOIN sys_customer c ON c.uuid = r.customer_uuid "
            + "LEFT JOIN sys_paper p ON p.uuid = r.paper_uuid "
            + "LEFT JOIN sys_user au ON au.uuid = e.acknowledged_by "
            + "LEFT JOIN sys_user iu ON iu.uuid = e.ignored_by ";
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM rpt_alert_event e " + JOINS;
    private static final String SUMMARY_SQL = "SELECT "
            + "SUM(CASE WHEN e.event_status = 1 THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN e.event_status = 2 THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN e.event_status = 3 THEN 1 ELSE 0 END) "
            + "FROM rpt_alert_event e " + JOINS;
    private static final String LIST_SQL = "SELECT e.uuid, r.rule_name, r.signal_code, "
            + "CASE r.scope_type WHEN 1 THEN '全局' WHEN 2 THEN CONCAT('客户：', COALESCE(c.customer_name, r.customer_uuid)) "
            + "WHEN 3 THEN CONCAT('纸张：', COALESCE(p.paper_name, r.paper_uuid)) "
            + "WHEN 4 THEN CONCAT('工艺：', CASE r.process_type WHEN 1 THEN '锯纸' WHEN 2 THEN '复卷' ELSE r.process_type END) END, "
            + "e.metric_release_uuid, r.comparison_operator, e.period_start, e.period_end, e.metric_value, e.threshold_value, e.severity, "
            + "e.event_status, e.occurrence_count, e.first_detected_at, e.last_detected_at, e.resolved_at, "
            + "e.acknowledged_at, COALESCE(au.real_name, au.username), e.ignored_at, COALESCE(iu.real_name, iu.username), e.ignore_reason "
            + "FROM rpt_alert_event e " + JOINS;
    private static final String ORDER_SQL = " ORDER BY e.last_detected_at DESC, e.uuid DESC LIMIT ? OFFSET ?";
    private static final RowMapper<ReportAlertEventVO> EVENT_ROW = (rs, row) -> new ReportAlertEventVO(
            rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
            rs.getObject(7, java.time.LocalDate.class), rs.getObject(8, java.time.LocalDate.class),
            rs.getBigDecimal(9), rs.getBigDecimal(10), rs.getInt(11), rs.getInt(12), rs.getInt(13),
            rs.getObject(14, java.time.LocalDateTime.class), rs.getObject(15, java.time.LocalDateTime.class),
            rs.getObject(16, java.time.LocalDateTime.class), rs.getObject(17, java.time.LocalDateTime.class),
            rs.getString(18), rs.getObject(19, java.time.LocalDateTime.class), rs.getString(20), rs.getString(21));
}
