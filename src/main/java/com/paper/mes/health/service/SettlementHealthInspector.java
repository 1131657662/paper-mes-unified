package com.paper.mes.health.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.health.dto.DataHealthIssueVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SettlementHealthInspector implements DataHealthInspector {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<DataHealthIssueVO> inspect() {
        List<DataHealthIssueVO> issues = new ArrayList<>(settlementTotalMismatches());
        issues.addAll(settledOrdersWithoutSettlement());
        issues.addAll(snapshotIntegrityIssues());
        return issues;
    }

    private List<DataHealthIssueVO> settlementTotalMismatches() {
        return jdbcTemplate.query(SETTLEMENT_TOTAL_SQL, (rs, rowNum) -> new DataHealthIssueVO(
                "SETTLEMENT_TOTAL_MISMATCH", "CRITICAL", "结算单",
                rs.getString("uuid"), rs.getString("settle_no"),
                "结算单主从金额不一致",
                "主表 " + rs.getBigDecimal("total_amount")
                        + "，明细合计 " + rs.getBigDecimal("detail_amount"),
                "RECONCILE_SETTLEMENT"
        ));
    }

    private List<DataHealthIssueVO> settledOrdersWithoutSettlement() {
        return jdbcTemplate.query(ORDER_WITHOUT_SETTLEMENT_SQL, (rs, rowNum) -> new DataHealthIssueVO(
                "SETTLED_ORDER_WITHOUT_SETTLEMENT", "CRITICAL", "加工单",
                rs.getString("uuid"), rs.getString("order_no"),
                "已结算加工单缺少结算明细",
                "客户 " + rs.getString("customer_name")
                        + "，加工应收 " + rs.getBigDecimal("total_amount"),
                "RESTORE_COMPLETED_ORDER"
        ));
    }

    private List<DataHealthIssueVO> snapshotIntegrityIssues() {
        return jdbcTemplate.query(SETTLEMENT_SNAPSHOT_SQL, (rs, rowNum) -> new SnapshotRow(
                        rs.getString("uuid"), rs.getString("settle_no"), rs.getString("snap_bill")))
                .stream()
                .filter(row -> !isValidSnapshot(row.snapshot()))
                .map(row -> new DataHealthIssueVO(
                        "SETTLEMENT_SNAPSHOT_CORRUPTED", "CRITICAL", "结算单",
                        row.uuid(), row.businessNo(), "结算单历史快照损坏",
                        "快照无法解析或缺少结算明细、打印明细，历史单据已停止读取", null))
                .toList();
    }

    private boolean isValidSnapshot(String snapshot) {
        try {
            JsonNode root = objectMapper.readTree(snapshot);
            return root != null && root.isObject()
                    && hasUsableItems(firstExisting(root, "detail_items", "detailItems", "details"))
                    && hasReadablePrintItems(firstExisting(root,
                    "print_line_items", "printLineItems", "print_lines", "printLines"));
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasUsableItems(JsonNode items) {
        if (items == null || !items.isArray() || items.isEmpty()) {
            return false;
        }
        for (JsonNode item : items) {
            if (hasText(item, "order_uuid", "orderUuid") || hasText(item, "order_no", "orderNo")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasReadablePrintItems(JsonNode items) {
        return items != null && items.isArray()
                && (items.isEmpty() || hasUsableItems(items));
    }

    private JsonNode firstExisting(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode value = root.get(name);
            if (value != null && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private boolean hasText(JsonNode root, String... names) {
        JsonNode value = firstExisting(root, names);
        return value != null && !value.asText().isBlank();
    }

    private static final String SETTLEMENT_TOTAL_SQL = """
            SELECT s.uuid, s.settle_no, s.total_amount, COALESCE(d.detail_amount, 0) detail_amount
            FROM biz_settle_order s
            LEFT JOIN (
                SELECT settle_uuid, SUM(order_amount) detail_amount
                FROM biz_settle_detail WHERE is_deleted = 0 GROUP BY settle_uuid
            ) d ON d.settle_uuid = s.uuid
            WHERE s.is_deleted = 0
              AND ABS(s.total_amount - COALESCE(d.detail_amount, 0)) > 0.01
            ORDER BY s.create_time DESC
            """;

    private static final String ORDER_WITHOUT_SETTLEMENT_SQL = """
            SELECT p.uuid, p.order_no, p.customer_name, p.total_amount
            FROM biz_process_order p
            WHERE p.is_deleted = 0 AND p.order_status = 5
              AND NOT EXISTS (
                SELECT 1 FROM biz_settle_detail d
                INNER JOIN biz_settle_order s ON s.uuid = d.settle_uuid AND s.is_deleted = 0
                WHERE d.order_uuid = p.uuid AND d.is_deleted = 0
              )
            ORDER BY p.update_time DESC
            """;

    private static final String SETTLEMENT_SNAPSHOT_SQL = """
            SELECT uuid, settle_no, snap_bill
            FROM biz_settle_order
            WHERE is_deleted = 0 AND snap_bill IS NOT NULL
              AND CHAR_LENGTH(TRIM(CAST(snap_bill AS CHAR))) > 0
            """;

    private record SnapshotRow(String uuid, String businessNo, String snapshot) {
    }
}
