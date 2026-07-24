package com.paper.mes.report.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportMetricVersionAuditVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReportLiveMetricRegistry {

    private static final Map<String, Integer> SUPPORTED = Map.ofEntries(
            Map.entry("order_count", 1), Map.entry("original_roll_count", 1),
            Map.entry("finish_roll_count", 1), Map.entry("original_weight_kg", 1),
            Map.entry("finish_weight_kg", 1), Map.entry("loss_weight_kg", 1),
            Map.entry("loss_ratio_pct", 1), Map.entry("knife_count", 1),
            Map.entry("saw_amount", 1), Map.entry("rewind_amount", 1),
            Map.entry("process_amount", 1), Map.entry("extra_amount", 1),
            Map.entry("total_amount", 1), Map.entry("settled_amount", 1),
            Map.entry("pending_settle_amount", 1), Map.entry("received_amount", 1),
            Map.entry("cash_received_amount", 1), Map.entry("scrap_offset_amount", 1),
            Map.entry("unreceived_amount", 1),
            Map.entry("settlement_document_count", 1), Map.entry("settlement_pending_count", 1),
            Map.entry("settlement_partial_count", 1), Map.entry("overdue_document_count", 1),
            Map.entry("overdue_amount", 1), Map.entry("collection_record_count", 1),
            Map.entry("discount_amount", 1), Map.entry("scrap_weight_kg", 1),
            Map.entry("inventory_roll_count", 1), Map.entry("inventory_available_count", 1),
            Map.entry("inventory_locked_count", 1), Map.entry("inventory_exception_count", 1),
            Map.entry("inventory_weight_kg", 1), Map.entry("inventory_locked_weight_kg", 1),
            Map.entry("delivery_document_count", 1), Map.entry("delivery_pending_count", 1),
            Map.entry("delivery_completed_count", 1), Map.entry("delivery_pending_weight_kg", 1),
            Map.entry("delivery_completed_weight_kg", 1));
    private static final Set<String> BASELINE_V1 = Set.of(
            "order_count", "original_roll_count", "finish_roll_count", "original_weight_kg",
            "finish_weight_kg", "loss_weight_kg", "loss_ratio_pct", "knife_count",
            "saw_amount", "rewind_amount", "process_amount", "extra_amount", "total_amount",
            "settled_amount", "pending_settle_amount", "received_amount",
            "cash_received_amount", "scrap_offset_amount", "unreceived_amount");

    public void requireExecutable(List<ReportMetricVersionAuditVO> metrics) {
        Map<String, ReportMetricVersionAuditVO> actual = metrics.stream().collect(Collectors.toMap(
                ReportMetricVersionAuditVO::metricCode, Function.identity(),
                (left, right) -> duplicate(left.metricCode())));
        if (!isSupportedBundle(actual.keySet())) {
            throw new BusinessException("指标发布包与实时 SQL 指标集合不一致");
        }
        actual.forEach((code, metric) -> requireVersion(metric, SUPPORTED.get(code)));
    }

    private boolean isSupportedBundle(Set<String> actual) {
        return actual.equals(SUPPORTED.keySet()) || actual.equals(BASELINE_V1);
    }

    private void requireVersion(ReportMetricVersionAuditVO metric, int supportedVersion) {
        String expectedKey = "report.sql." + metric.metricCode();
        if (metric.versionStatus() != 2 || metric.versionNo() != supportedVersion
                || !expectedKey.equals(metric.implementationKey())) {
            throw new BusinessException("指标版本暂不支持在线执行: " + metric.metricCode());
        }
    }

    private ReportMetricVersionAuditVO duplicate(String metricCode) {
        throw new BusinessException("指标发布包包含重复指标: " + metricCode);
    }
}
