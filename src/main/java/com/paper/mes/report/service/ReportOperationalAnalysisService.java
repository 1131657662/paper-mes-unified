package com.paper.mes.report.service;

import com.paper.mes.report.dto.ReportCollectionAnalysisVO;
import com.paper.mes.report.dto.ReportDeliveryAnalysisVO;
import com.paper.mes.report.dto.ReportInventoryAnalysisVO;
import com.paper.mes.report.dto.ReportSettlementAnalysisVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.mapper.ReportOperationalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportOperationalAnalysisService {
    private static final Set<String> SETTLEMENT_METRICS = Set.of(
            "settlement_document_count", "settlement_pending_count", "settlement_partial_count",
            "overdue_document_count", "overdue_amount", "settled_amount", "received_amount",
            "unreceived_amount");
    private static final Set<String> COLLECTION_METRICS = Set.of(
            "collection_record_count", "received_amount", "cash_received_amount",
            "scrap_offset_amount", "discount_amount", "scrap_weight_kg");
    private static final Set<String> INVENTORY_METRICS = Set.of(
            "inventory_roll_count", "inventory_available_count", "inventory_locked_count",
            "inventory_exception_count", "inventory_weight_kg", "inventory_locked_weight_kg");
    private static final Set<String> DELIVERY_METRICS = Set.of(
            "delivery_document_count", "delivery_pending_count", "delivery_completed_count",
            "delivery_pending_weight_kg", "delivery_completed_weight_kg");

    private final ReportOperationalMapper mapper;
    private final ReportQueryCoordinator queryCoordinator;
    private final ReportOperationalQueryPolicy queryPolicy;

    @Transactional(readOnly = true)
    public ReportSettlementAnalysisVO settlement(ReportQuery query) {
        queryPolicy.requireSettlement(query);
        var metadata = queryCoordinator.prepare(query, SETTLEMENT_METRICS);
        return new ReportSettlementAnalysisVO("settlement", mapper.settlementOverview(query),
                mapper.settlementMonthly(query), mapper.settlementCustomers(query), metadata.dataAsOf(), metadata);
    }

    @Transactional(readOnly = true)
    public ReportCollectionAnalysisVO collection(ReportQuery query) {
        queryPolicy.requireCollection(query);
        var metadata = queryCoordinator.prepare(query, COLLECTION_METRICS);
        return new ReportCollectionAnalysisVO("collection", mapper.collectionOverview(query),
                mapper.collectionMonthly(query), mapper.collectionCustomers(query), metadata.dataAsOf(), metadata);
    }

    @Transactional(readOnly = true)
    public ReportInventoryAnalysisVO inventory(ReportQuery query) {
        queryPolicy.requireInventory(query);
        var metadata = queryCoordinator.prepare(query, INVENTORY_METRICS);
        return new ReportInventoryAnalysisVO("inventory", "CURRENT_STOCK_BY_STOCK_IN_MONTH",
                mapper.inventoryOverview(query), mapper.inventoryMonthly(query),
                mapper.inventoryWarehouses(query), metadata.dataAsOf(), metadata);
    }

    @Transactional(readOnly = true)
    public ReportDeliveryAnalysisVO delivery(ReportQuery query) {
        queryPolicy.requireDelivery(query);
        var metadata = queryCoordinator.prepare(query, DELIVERY_METRICS);
        return new ReportDeliveryAnalysisVO("delivery", mapper.deliveryOverview(query),
                mapper.deliveryMonthly(query), mapper.deliveryWarehouses(query), metadata.dataAsOf(), metadata);
    }
}
