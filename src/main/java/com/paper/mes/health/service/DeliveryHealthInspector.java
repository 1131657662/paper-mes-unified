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
public class DeliveryHealthInspector implements DataHealthInspector {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<DataHealthIssueVO> inspect() {
        List<DataHealthIssueVO> issues = new ArrayList<>();
        issues.addAll(deliveryTotalMismatches());
        issues.addAll(stockLockMismatches());
        issues.addAll(orphanDetails());
        issues.addAll(snapshotIntegrityIssues());
        return issues;
    }

    private List<DataHealthIssueVO> deliveryTotalMismatches() {
        return jdbcTemplate.query(DELIVERY_TOTAL_SQL, (rs, rowNum) -> issue(
                "DELIVERY_TOTAL_MISMATCH", "CRITICAL", rs.getString("uuid"),
                rs.getString("delivery_no"), "出库单合计与明细不一致",
                "主表 " + rs.getInt("total_count") + " 卷 / " + rs.getBigDecimal("total_weight")
                        + " kg，明细 " + rs.getInt("detail_count") + " 卷 / "
                        + rs.getBigDecimal("detail_weight") + " kg"));
    }

    private List<DataHealthIssueVO> stockLockMismatches() {
        return jdbcTemplate.query(STOCK_LOCK_SQL, (rs, rowNum) -> issue(
                "DELIVERY_STOCK_LOCK_MISMATCH", "CRITICAL", rs.getString("uuid"),
                rs.getString("delivery_no"), "出库状态与库存锁不一致",
                "出库状态 " + rs.getInt("delivery_status")
                        + "，异常明细 " + rs.getInt("mismatch_count") + " 条"));
    }

    private List<DataHealthIssueVO> orphanDetails() {
        return jdbcTemplate.query(ORPHAN_DETAIL_SQL, (rs, rowNum) -> issue(
                "ORPHAN_DELIVERY_DETAIL", "CRITICAL", rs.getString("uuid"),
                rs.getString("finish_roll_no"), "出库明细来源关联缺失",
                "明细无法关联有效出库单或成品卷"));
    }

    private List<DataHealthIssueVO> snapshotIntegrityIssues() {
        return jdbcTemplate.query(DELIVERY_SNAPSHOT_SQL, (rs, rowNum) -> new SnapshotRow(
                        rs.getString("uuid"), rs.getString("delivery_no"), rs.getString("snap_delivery")))
                .stream()
                .filter(row -> !isValidSnapshot(row.snapshot()))
                .map(row -> issue("DELIVERY_SNAPSHOT_CORRUPTED", "CRITICAL", row.uuid(), row.businessNo(),
                        "出库单历史快照损坏", "快照无法解析或缺少可追溯的出库明细，历史单据已停止读取"))
                .toList();
    }

    private boolean isValidSnapshot(String snapshot) {
        try {
            JsonNode root = objectMapper.readTree(snapshot);
            if (root == null || !root.isObject()) {
                return false;
            }
            JsonNode type = firstExisting(root, "snapshot_type", "snapshotType");
            if (type != null && "delivery_rollback".equals(type.asText())) {
                return hasUsableItems(firstExisting(root,
                        "previous_confirm_snapshot", "previousConfirmSnapshot"));
            }
            return hasUsableItems(root);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasUsableItems(JsonNode root) {
        if (root == null || !root.isObject()) {
            return false;
        }
        JsonNode items = firstExisting(root, "detail_items", "detailItems", "details");
        if (items == null || !items.isArray() || items.isEmpty()) {
            return false;
        }
        for (JsonNode item : items) {
            if (hasText(item, "finish_uuid", "finishUuid")
                    || hasText(item, "finish_roll_no", "finishRollNo")) {
                return true;
            }
        }
        return false;
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

    private DataHealthIssueVO issue(String type, String severity, String uuid,
                                    String no, String title, String detail) {
        return new DataHealthIssueVO(type, severity, "出库单", uuid, no, title, detail, null);
    }

    private static final String DELIVERY_TOTAL_SQL = """
            SELECT o.uuid, o.delivery_no, o.total_count, o.total_weight,
                   COALESCE(d.detail_count, 0) detail_count,
                   COALESCE(d.detail_weight, 0) detail_weight
            FROM biz_delivery_order o
            LEFT JOIN (
                SELECT delivery_uuid, COUNT(*) detail_count, SUM(out_weight) detail_weight
                FROM biz_delivery_detail WHERE is_deleted = 0 GROUP BY delivery_uuid
            ) d ON d.delivery_uuid = o.uuid
            WHERE o.is_deleted = 0 AND (
                o.total_count <> COALESCE(d.detail_count, 0)
                OR ABS(o.total_weight - COALESCE(d.detail_weight, 0)) > 0.001
            )
            """;

    private static final String STOCK_LOCK_SQL = """
            SELECT o.uuid, o.delivery_no, o.delivery_status, COUNT(*) mismatch_count
            FROM biz_delivery_order o
            INNER JOIN biz_delivery_detail d ON d.delivery_uuid = o.uuid AND d.is_deleted = 0
            WHERE o.is_deleted = 0
              AND ((o.delivery_status = 1 AND d.stock_lock_status <> 1)
                OR (o.delivery_status = 2 AND d.stock_lock_status <> 0))
            GROUP BY o.uuid, o.delivery_no, o.delivery_status
            """;

    private static final String ORPHAN_DETAIL_SQL = """
            SELECT d.uuid, d.finish_roll_no
            FROM biz_delivery_detail d
            LEFT JOIN biz_delivery_order o ON o.uuid = d.delivery_uuid AND o.is_deleted = 0
            LEFT JOIN biz_finish_roll f ON f.uuid = d.finish_uuid AND f.is_deleted = 0
            WHERE d.is_deleted = 0 AND (o.uuid IS NULL OR f.uuid IS NULL)
            """;

    private static final String DELIVERY_SNAPSHOT_SQL = """
            SELECT uuid, delivery_no, snap_delivery
            FROM biz_delivery_order
            WHERE is_deleted = 0 AND snap_delivery IS NOT NULL
              AND CHAR_LENGTH(TRIM(CAST(snap_delivery AS CHAR))) > 0
            """;

    private record SnapshotRow(String uuid, String businessNo, String snapshot) {
    }
}
