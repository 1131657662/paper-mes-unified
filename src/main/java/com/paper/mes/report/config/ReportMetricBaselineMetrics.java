package com.paper.mes.report.config;

import java.util.List;

final class ReportMetricBaselineMetrics {

    private ReportMetricBaselineMetrics() {
    }

    static List<MetricSeed> all() {
        return java.util.stream.Stream.concat(List.of(
                metric("order_count", "加工单数", "符合筛选条件的有效加工单数量", "INTEGER", "ORDER", 0, 10),
                metric("original_roll_count", "原卷数", "非直发有效原卷数量", "INTEGER", "ROLL", 0, 20),
                metric("finish_roll_count", "成品数", "有效最终成品数量", "INTEGER", "ROLL", 0, 30),
                metric("original_weight_kg", "原纸重量", "有效原卷实际优先重量，单位千克", "DECIMAL", "KG", 3, 40),
                metric("finish_weight_kg", "成品重量", "有效最终成品实际优先重量，单位千克", "DECIMAL", "KG", 3, 50),
                metric("loss_weight_kg", "损耗重量", "原卷累计损耗重量，单位千克", "DECIMAL", "KG", 3, 60),
                metric("loss_ratio_pct", "损耗率", "损耗重量除以原纸重量乘以100", "PERCENT", "PERCENT", 2, 70),
                metric("knife_count", "刀数", "有效加工工序刀数合计", "INTEGER", "KNIFE", 0, 80),
                metric("saw_amount", "锯纸费", "锯纸工序费用合计", "MONEY", "CNY", 2, 90),
                metric("rewind_amount", "复卷费", "复卷工序费用合计", "MONEY", "CNY", 2, 100),
                metric("process_amount", "加工费", "加工费用合计", "MONEY", "CNY", 2, 110),
                metric("extra_amount", "附加费", "加工单附加费用合计", "MONEY", "CNY", 2, 120),
                metric("total_amount", "应收合计", "有效应收金额合计", "MONEY", "CNY", 2, 130),
                metric("settled_amount", "已结算应收", "有效结算单覆盖的应收金额", "MONEY", "CNY", 2, 140),
                metric("pending_settle_amount", "待结算应收", "尚未进入有效结算单的应收金额", "MONEY", "CNY", 2, 150),
                metric("received_amount", "已收金额", "有效收款流水总额", "MONEY", "CNY", 2, 160),
                metric("cash_received_amount", "现金到账", "有效收款流水中的现金金额", "MONEY", "CNY", 2, 170),
                metric("scrap_offset_amount", "废纸抵扣", "有效收款流水中的废纸抵扣金额", "MONEY", "CNY", 2, 180),
                metric("unreceived_amount", "已结算未收", "已结算应收减有效已收金额，不小于零", "MONEY", "CNY", 2, 190)
        ).stream(), operational().stream()).toList();
    }

    private static List<MetricSeed> operational() {
        return List.of(
                metric("settlement_document_count", "有效结算单数", "未作废的结算单数量", "INTEGER", "DOCUMENT", 0, 200),
                metric("settlement_pending_count", "待收款结算单数", "状态为待收款的有效结算单数量", "INTEGER", "DOCUMENT", 0, 210),
                metric("settlement_partial_count", "部分收款结算单数", "状态为部分收款的有效结算单数量", "INTEGER", "DOCUMENT", 0, 220),
                metric("overdue_document_count", "逾期结算单数", "到期且仍有欠款的有效结算单数量", "INTEGER", "DOCUMENT", 0, 230),
                metric("overdue_amount", "逾期金额", "逾期有效结算单的未收金额", "MONEY", "CNY", 2, 240),
                metric("collection_record_count", "有效回款流水数", "未撤销的有效回款流水数量", "INTEGER", "RECORD", 0, 250),
                metric("discount_amount", "优惠核销金额", "有效回款流水中的优惠及尾差核销金额", "MONEY", "CNY", 2, 260),
                metric("scrap_weight_kg", "废纸抵扣重量", "有效回款流水中的废纸抵扣重量", "DECIMAL", "KG", 3, 270),
                metric("inventory_roll_count", "当前库存卷数", "查询范围内当前在库成品卷数量", "INTEGER", "ROLL", 0, 280),
                metric("inventory_available_count", "可用库存卷数", "当前在库且未被待出库单锁定的成品卷数量", "INTEGER", "ROLL", 0, 290),
                metric("inventory_locked_count", "锁定库存卷数", "当前在库且被待出库单锁定的成品卷数量", "INTEGER", "ROLL", 0, 300),
                metric("inventory_exception_count", "异常库存卷数", "仓库或入库时间缺失以及异常的当前库存卷数量", "INTEGER", "ROLL", 0, 310),
                metric("inventory_weight_kg", "当前库存重量", "当前在库成品卷剩余可出库重量", "DECIMAL", "KG", 3, 320),
                metric("inventory_locked_weight_kg", "锁定库存重量", "被待出库单锁定的当前库存重量", "DECIMAL", "KG", 3, 330),
                metric("delivery_document_count", "有效出库单数", "待出库与已签收的有效出库单数量", "INTEGER", "DOCUMENT", 0, 340),
                metric("delivery_pending_count", "待出库单数", "状态为待出库的有效出库单数量", "INTEGER", "DOCUMENT", 0, 350),
                metric("delivery_completed_count", "已出库单数", "状态为已出库签收的有效出库单数量", "INTEGER", "DOCUMENT", 0, 360),
                metric("delivery_pending_weight_kg", "待出库重量", "待出库单有效明细的出库重量", "DECIMAL", "KG", 3, 370),
                metric("delivery_completed_weight_kg", "已出库重量", "已出库签收单有效明细的出库重量", "DECIMAL", "KG", 3, 380)
        );
    }

    private static MetricSeed metric(String code, String name, String description,
                                     String valueType, String unitCode, int scale, int order) {
        return new MetricSeed(code, name, description, valueType, unitCode, scale, order);
    }

    record MetricSeed(String code, String name, String description, String valueType,
                      String unitCode, int scale, int order) {
    }
}
