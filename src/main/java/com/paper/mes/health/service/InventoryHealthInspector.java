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
                rs.getLong("roll_count") + " 卷，剩余重量 " + rs.getBigDecimal("remaining_weight")
                        + " kg；请在成品库存的未分仓治理中人工确认仓库",
                "OPEN_INVENTORY_WAREHOUSE_REPAIR"));
    }

    private static final String UNASSIGNED_FINISH_SQL = """
            SELECT o.uuid order_uuid, o.order_no,
                   COUNT(*) roll_count,
                   COALESCE(SUM(COALESCE(f.remaining_weight, f.actual_weight, 0)), 0) remaining_weight
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
