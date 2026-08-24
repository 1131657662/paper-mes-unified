package com.paper.mes.health.service;

import com.paper.mes.health.dto.DataHealthIssueVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** 监测已完成加工单中仍未确认仓库的历史成品。 */
@Component
@RequiredArgsConstructor
public class InventoryHealthInspector implements DataHealthInspector {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<DataHealthIssueVO> inspect() {
        return jdbcTemplate.query(UNASSIGNED_FINISH_SQL, (rs, rowNum) -> new DataHealthIssueVO(
                "UNASSIGNED_FINISH_WAREHOUSE", "WARNING", "加工单", rs.getString("order_uuid"),
                rs.getString("order_no"), "历史成品缺少仓库归属",
                rs.getLong("roll_count") + " 卷，剩余重量 " + weightText(rs.getBigDecimal("remaining_weight"))
                        + unknownText(rs.getLong("unknown_weight_count"))
                        + "；请在成品库存的未分仓治理中人工确认仓库",
                "OPEN_INVENTORY_WAREHOUSE_REPAIR"));
    }

    private String weightText(java.math.BigDecimal value) {
        return value == null ? "待称重" : value.stripTrailingZeros().toPlainString() + " kg";
    }

    private String unknownText(long count) {
        return count <= 0 ? "" : "，其中 " + count + " 卷重量未知";
    }

    private static final String UNASSIGNED_FINISH_SQL = """
            SELECT o.uuid order_uuid, o.order_no,
                   COUNT(*) roll_count,
                   SUM(CASE WHEN COALESCE(f.actual_weight, 0) > 0 OR NOT EXISTS (
                                SELECT 1
                                FROM biz_finish_original_rel fr
                                INNER JOIN biz_original_roll r
                                  ON r.uuid = fr.original_uuid AND r.is_deleted = 0
                                WHERE fr.finish_uuid = f.uuid AND fr.is_deleted = 0
                                  AND r.weight_status = 'UNKNOWN'
                                  AND COALESCE(r.actual_weight, 0) <= 0
                            )
                            THEN COALESCE(f.remaining_weight, f.actual_weight, 0)
                            ELSE NULL END) remaining_weight,
                   SUM(CASE WHEN COALESCE(f.actual_weight, 0) <= 0 AND EXISTS (
                                SELECT 1
                                FROM biz_finish_original_rel fr
                                INNER JOIN biz_original_roll r
                                  ON r.uuid = fr.original_uuid AND r.is_deleted = 0
                                WHERE fr.finish_uuid = f.uuid AND fr.is_deleted = 0
                                  AND r.weight_status = 'UNKNOWN'
                                  AND COALESCE(r.actual_weight, 0) <= 0
                            ) THEN 1 ELSE 0 END) unknown_weight_count
            FROM biz_finish_roll f
            INNER JOIN biz_process_order o ON o.uuid = f.order_uuid AND o.is_deleted = 0
            WHERE f.is_deleted = 0
              AND f.finish_status IN (2, 3)
              AND f.warehouse_uuid IS NULL
              AND o.order_status IN (4, 5)
            GROUP BY o.uuid, o.order_no
            ORDER BY o.order_no ASC
            """;
}
