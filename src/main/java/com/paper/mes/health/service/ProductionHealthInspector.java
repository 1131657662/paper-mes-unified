package com.paper.mes.health.service;

import com.paper.mes.health.dto.DataHealthIssueVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductionHealthInspector {

    private final JdbcTemplate jdbcTemplate;
    private final ProductionCompletionHealthInspector completionInspector;

    public List<DataHealthIssueVO> inspect() {
        List<DataHealthIssueVO> issues = new ArrayList<>();
        issues.addAll(completionInspector.inspect());
        issues.addAll(orderWeightMismatches());
        issues.addAll(massBalanceGainIssues());
        issues.addAll(finishWeightRangeIssues());
        issues.addAll(onSiteFinishWidthIssues());
        issues.addAll(missingSourceRelations());
        issues.addAll(missingMotherRollNumbers());
        return issues;
    }

    private List<DataHealthIssueVO> orderWeightMismatches() {
        return jdbcTemplate.query(ORDER_WEIGHT_SQL, (rs, rowNum) -> issue(
                "ORDER_WEIGHT_SUMMARY_MISMATCH", "WARNING", rs.getString("uuid"),
                rs.getString("order_no"), "加工单重量汇总与明细不一致",
                "原纸 " + rs.getBigDecimal("order_original") + " / " + rs.getBigDecimal("detail_original")
                        + " kg，成品 " + rs.getBigDecimal("order_finish") + " / "
                        + rs.getBigDecimal("detail_finish") + " kg", null));
    }

    private List<DataHealthIssueVO> finishWeightRangeIssues() {
        return jdbcTemplate.query(FINISH_WEIGHT_SQL, (rs, rowNum) -> issue(
                "FINISH_REMAINING_WEIGHT_INVALID", "CRITICAL", rs.getString("uuid"),
                rs.getString("finish_roll_no"), "成品剩余可出库重量越界",
                "实重 " + rs.getBigDecimal("actual_weight") + " kg，剩余 "
                        + rs.getBigDecimal("remaining_weight") + " kg", null));
    }

    private List<DataHealthIssueVO> massBalanceGainIssues() {
        return jdbcTemplate.query(MASS_BALANCE_GAIN_SQL, (rs, rowNum) -> issue(
                "ORDER_MASS_BALANCE_GAIN", "CRITICAL", rs.getString("uuid"),
                rs.getString("order_no"), "成品总重超过原纸总重",
                "原纸 " + rs.getBigDecimal("original_weight") + " kg，成品 "
                        + rs.getBigDecimal("finish_weight") + " kg，超出 "
                        + rs.getBigDecimal("gain_weight") + " kg", null));
    }

    private List<DataHealthIssueVO> onSiteFinishWidthIssues() {
        return jdbcTemplate.query(ON_SITE_FINISH_WIDTH_SQL, (rs, rowNum) -> issue(
                "ONSITE_FINISH_WIDTH_INVALID", "CRITICAL", rs.getString("uuid"),
                rs.getString("finish_roll_no"), "现场定尺成品门幅无效",
                "加工单 " + rs.getString("order_no") + "，成品已进入库存但门幅未完成现场回录", null));
    }

    private List<DataHealthIssueVO> missingSourceRelations() {
        return jdbcTemplate.query(MISSING_RELATION_SQL, (rs, rowNum) -> issue(
                "FINISH_SOURCE_RELATION_MISSING", "WARNING", rs.getString("uuid"),
                rs.getString("finish_roll_no"), "成品卷缺少来源母卷关联",
                "加工单 " + rs.getString("order_no") + "，需要补录来源关系", null));
    }

    private List<DataHealthIssueVO> missingMotherRollNumbers() {
        return jdbcTemplate.query(MISSING_ROLL_NO_SQL, (rs, rowNum) -> issue(
                "MOTHER_ROLL_NUMBER_MISSING", "WARNING", rs.getString("uuid"),
                rs.getString("order_no"), "母卷卷号待补",
                "该加工单有 " + rs.getInt("missing_count") + " 卷母卷未记录卷号", null));
    }

    private DataHealthIssueVO issue(String type, String severity, String uuid, String no,
                                    String title, String detail, String repairAction) {
        return new DataHealthIssueVO(type, severity, "加工单", uuid, no, title, detail, repairAction);
    }

    private static final String ORDER_WEIGHT_SQL = """
            SELECT p.uuid, p.order_no, p.total_original_weight order_original,
                   p.total_finish_weight order_finish,
                   COALESCE(o.original_weight, 0) detail_original,
                   COALESCE(f.finish_weight, 0) detail_finish
            FROM biz_process_order p
            LEFT JOIN (
                SELECT order_uuid, SUM(COALESCE(actual_weight, total_weight, roll_weight, 0)) original_weight
                FROM biz_original_roll WHERE is_deleted = 0 GROUP BY order_uuid
            ) o ON o.order_uuid = p.uuid
            LEFT JOIN (
                SELECT order_uuid, SUM(COALESCE(actual_weight, estimate_weight, 0)) finish_weight
                FROM biz_finish_roll
                WHERE is_deleted = 0 AND roll_no_status <> 3 AND is_spare = 0 GROUP BY order_uuid
            ) f ON f.order_uuid = p.uuid
            WHERE p.is_deleted = 0 AND p.order_status IN (4, 5)
              AND (ABS(COALESCE(p.total_original_weight, 0) - COALESCE(o.original_weight, 0)) > 0.001
                OR ABS(COALESCE(p.total_finish_weight, 0) - COALESCE(f.finish_weight, 0)) > 0.001)
            ORDER BY p.update_time DESC
            """;

    private static final String FINISH_WEIGHT_SQL = """
            SELECT uuid, finish_roll_no, actual_weight, remaining_weight
            FROM biz_finish_roll
            WHERE is_deleted = 0 AND (remaining_weight < 0
              OR (actual_weight IS NOT NULL AND remaining_weight > actual_weight))
            """;

    private static final String MASS_BALANCE_GAIN_SQL = """
            SELECT p.uuid, p.order_no, o.original_weight, f.finish_weight,
                   f.finish_weight - o.original_weight gain_weight
            FROM biz_process_order p
            INNER JOIN (
                SELECT order_uuid, SUM(COALESCE(actual_weight, total_weight, roll_weight, 0)) original_weight
                FROM biz_original_roll WHERE is_deleted = 0 GROUP BY order_uuid
            ) o ON o.order_uuid = p.uuid
            INNER JOIN (
                SELECT order_uuid, SUM(COALESCE(actual_weight, estimate_weight, 0)) finish_weight
                FROM biz_finish_roll
                WHERE is_deleted = 0 AND roll_no_status <> 3 AND is_spare = 0 GROUP BY order_uuid
            ) f ON f.order_uuid = p.uuid
            WHERE p.is_deleted = 0 AND p.order_status IN (4, 5)
              AND f.finish_weight > o.original_weight + 0.001
            ORDER BY gain_weight DESC
            """;

    private static final String ON_SITE_FINISH_WIDTH_SQL = """
            SELECT f.uuid, f.finish_roll_no, p.order_no
            FROM biz_finish_roll f
            INNER JOIN biz_process_order p ON p.uuid = f.order_uuid AND p.is_deleted = 0
            WHERE f.is_deleted = 0 AND f.source_type = 1 AND f.roll_no_status <> 3
              AND f.finish_status IN (2, 3) AND COALESCE(f.finish_width, 0) <= 0
              AND EXISTS (
                SELECT 1
                FROM biz_finish_original_rel rel
                INNER JOIN biz_original_roll original
                  ON original.uuid = rel.original_uuid AND original.is_deleted = 0
                WHERE rel.finish_uuid = f.uuid AND rel.is_deleted = 0
                  AND original.process_mode = 2
              )
            ORDER BY f.create_time DESC
            """;

    private static final String MISSING_RELATION_SQL = """
            SELECT f.uuid, f.finish_roll_no, p.order_no
            FROM biz_finish_roll f
            INNER JOIN biz_process_order p ON p.uuid = f.order_uuid AND p.is_deleted = 0
            WHERE f.is_deleted = 0 AND f.source_type = 1 AND f.is_spare = 0
              AND f.roll_no_status <> 3 AND f.finish_status IN (2, 3)
              AND NOT EXISTS (
                SELECT 1 FROM biz_finish_original_rel r
                WHERE r.finish_uuid = f.uuid AND r.is_deleted = 0
              )
            ORDER BY f.create_time DESC
            """;

    private static final String MISSING_ROLL_NO_SQL = """
            SELECT p.uuid, p.order_no, COUNT(*) missing_count
            FROM biz_process_order p
            INNER JOIN biz_original_roll r ON r.order_uuid = p.uuid AND r.is_deleted = 0
            WHERE p.is_deleted = 0 AND p.order_status IN (1, 2, 3, 4, 5)
              AND (r.roll_no IS NULL OR TRIM(r.roll_no) = '')
            GROUP BY p.uuid, p.order_no
            ORDER BY p.update_time DESC
            """;
}
