package com.paper.mes.health.service;

import com.paper.mes.health.config.DataHealthProperties;
import com.paper.mes.health.dto.DataHealthIssueVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkflowHealthInspector {

    private final JdbcTemplate jdbcTemplate;
    private final DataHealthProperties properties;

    public List<DataHealthIssueVO> inspect() {
        List<DataHealthIssueVO> issues = new ArrayList<>(overdueBackRecords());
        issues.addAll(overdueReceivables());
        return issues;
    }

    private List<DataHealthIssueVO> overdueBackRecords() {
        return jdbcTemplate.query(OVERDUE_BACK_RECORD_SQL, (rs, rowNum) -> new DataHealthIssueVO(
                "OVERDUE_BACK_RECORD", "WARNING", "加工单", rs.getString("uuid"),
                rs.getString("order_no"), "待回录加工单已超期",
                "进入待回录后已超过 " + properties.getBackRecordOverdueDays() + " 天，请及时完成生产回录",
                null), properties.getBackRecordOverdueDays());
    }

    private List<DataHealthIssueVO> overdueReceivables() {
        return jdbcTemplate.query(OVERDUE_RECEIVABLE_SQL, (rs, rowNum) -> new DataHealthIssueVO(
                "OVERDUE_RECEIVABLE", "WARNING", "结算单", rs.getString("uuid"),
                rs.getString("settle_no"), "已结算未收款已超期",
                "未收 " + rs.getBigDecimal("unreceived_amount") + " 元，已超过 "
                        + properties.getReceivableOverdueDays() + " 天，请及时跟进回款",
                null), properties.getReceivableOverdueDays());
    }

    private static final String OVERDUE_BACK_RECORD_SQL = """
            SELECT uuid, order_no FROM biz_process_order
            WHERE is_deleted = 0 AND order_status = 3
              AND update_time < TIMESTAMPADD(DAY, -?, NOW())
            ORDER BY update_time ASC
            """;

    private static final String OVERDUE_RECEIVABLE_SQL = """
            SELECT uuid, settle_no, unreceived_amount FROM biz_settle_order
            WHERE is_deleted = 0 AND settle_status IN (1, 2) AND unreceived_amount > 0
              AND settle_date < TIMESTAMPADD(DAY, -?, CURDATE())
            ORDER BY settle_date ASC
            """;
}
