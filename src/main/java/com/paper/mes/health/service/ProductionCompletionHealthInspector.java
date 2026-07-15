package com.paper.mes.health.service;

import com.paper.mes.health.dto.DataHealthIssueVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductionCompletionHealthInspector {

    private final JdbcTemplate jdbcTemplate;

    public List<DataHealthIssueVO> inspect() {
        List<DataHealthIssueVO> issues = new ArrayList<>();
        for (CompletionState state : states()) {
            addOriginalWeightIssue(issues, state);
            addFinishWeightIssue(issues, state);
            addPendingInventoryIssue(issues, state);
            addMissingOutputIssue(issues, state);
        }
        return issues;
    }

    private List<CompletionState> states() {
        return jdbcTemplate.query(SQL, (rs, rowNum) -> new CompletionState(
                rs.getString("uuid"), rs.getString("order_no"),
                rs.getInt("original_count"), rs.getInt("missing_original_weight"),
                rs.getInt("finish_count"), rs.getInt("missing_finish_weight"),
                rs.getInt("pending_finish_count"), rs.getInt("valid_output_count")));
    }

    private void addOriginalWeightIssue(List<DataHealthIssueVO> issues, CompletionState state) {
        if (state.originalCount() > 0 && state.missingOriginalWeight() == 0) return;
        String detail = state.originalCount() == 0 ? "已完成加工单没有母卷明细"
                : "共有 " + state.missingOriginalWeight() + " 卷母卷未填写复称实际重量";
        issues.add(issue("COMPLETED_ORIGINAL_WEIGHT_MISSING", state,
                "已完成加工单母卷回录不完整", detail));
    }

    private void addFinishWeightIssue(List<DataHealthIssueVO> issues, CompletionState state) {
        if (state.missingFinishWeight() == 0) return;
        issues.add(issue("COMPLETED_FINISH_WEIGHT_MISSING", state,
                "已完成加工单成品重量不完整",
                "共有 " + state.missingFinishWeight() + " 件正式成品未填写实际重量"));
    }

    private void addPendingInventoryIssue(List<DataHealthIssueVO> issues, CompletionState state) {
        if (state.pendingFinishCount() == 0) return;
        issues.add(issue("COMPLETED_FINISH_PENDING_INBOUND", state,
                "已完成加工单仍有成品待入库",
                "共有 " + state.pendingFinishCount() + " 件正式成品仍处于待入库状态"));
    }

    private void addMissingOutputIssue(List<DataHealthIssueVO> issues, CompletionState state) {
        if (state.validOutputCount() > 0) return;
        issues.add(issue("COMPLETED_ORDER_WITHOUT_ACTUAL_OUTPUT", state,
                "已完成加工单没有有效实际产出",
                "正式成品 " + state.finishCount() + " 件，但没有任何成品具备有效实际重量"));
    }

    private DataHealthIssueVO issue(String type, CompletionState state, String title, String detail) {
        return new DataHealthIssueVO(type, "CRITICAL", "加工单", state.uuid(), state.orderNo(),
                title, detail, null);
    }

    private record CompletionState(String uuid, String orderNo,
                                   int originalCount, int missingOriginalWeight,
                                   int finishCount, int missingFinishWeight,
                                   int pendingFinishCount, int validOutputCount) {
    }

    private static final String SQL = """
            SELECT p.uuid, p.order_no,
                   COALESCE(o.original_count, 0) original_count,
                   COALESCE(o.missing_weight, 0) missing_original_weight,
                   COALESCE(f.finish_count, 0) finish_count,
                   COALESCE(f.missing_weight, 0) missing_finish_weight,
                   COALESCE(f.pending_count, 0) pending_finish_count,
                   COALESCE(f.valid_output_count, 0) valid_output_count
            FROM biz_process_order p
            LEFT JOIN (
                SELECT order_uuid, COUNT(*) original_count,
                       SUM(CASE WHEN COALESCE(actual_weight, 0) <= 0 THEN 1 ELSE 0 END) missing_weight
                FROM biz_original_roll WHERE is_deleted = 0 GROUP BY order_uuid
            ) o ON o.order_uuid = p.uuid
            LEFT JOIN (
                SELECT order_uuid, COUNT(*) finish_count,
                       SUM(CASE WHEN COALESCE(actual_weight, 0) <= 0 THEN 1 ELSE 0 END) missing_weight,
                       SUM(CASE WHEN finish_status = 1 THEN 1 ELSE 0 END) pending_count,
                       SUM(CASE WHEN COALESCE(actual_weight, 0) > 0 THEN 1 ELSE 0 END) valid_output_count
                FROM biz_finish_roll
                WHERE is_deleted = 0 AND COALESCE(is_spare, 0) = 0
                  AND COALESCE(is_remain, 0) = 0 AND COALESCE(roll_no_status, 1) <> 3
                GROUP BY order_uuid
            ) f ON f.order_uuid = p.uuid
            WHERE p.is_deleted = 0 AND p.order_status IN (4, 5)
              AND (COALESCE(o.original_count, 0) = 0 OR COALESCE(o.missing_weight, 0) > 0
                OR COALESCE(f.missing_weight, 0) > 0 OR COALESCE(f.pending_count, 0) > 0
                OR COALESCE(f.valid_output_count, 0) = 0)
            ORDER BY p.update_time DESC
            """;
}
