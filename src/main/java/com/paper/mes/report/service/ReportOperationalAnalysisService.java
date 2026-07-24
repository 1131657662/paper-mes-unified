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

@Service
@RequiredArgsConstructor
public class ReportOperationalAnalysisService {
    private final ReportOperationalMapper mapper;
    private final ReportQueryCoordinator queryCoordinator;
    private final ReportOperationalQueryPolicy queryPolicy;

    @Transactional(readOnly = true)
    public ReportSettlementAnalysisVO settlement(ReportQuery query) {
        queryPolicy.requireSettlement(query);
        var metadata = queryCoordinator.prepare(query);
        return new ReportSettlementAnalysisVO("settlement", mapper.settlementOverview(query),
                mapper.settlementMonthly(query), mapper.settlementCustomers(query), metadata.dataAsOf());
    }

    @Transactional(readOnly = true)
    public ReportCollectionAnalysisVO collection(ReportQuery query) {
        queryPolicy.requireCollection(query);
        var metadata = queryCoordinator.prepare(query);
        return new ReportCollectionAnalysisVO("collection", mapper.collectionOverview(query),
                mapper.collectionMonthly(query), mapper.collectionCustomers(query), metadata.dataAsOf());
    }

    @Transactional(readOnly = true)
    public ReportInventoryAnalysisVO inventory(ReportQuery query) {
        queryPolicy.requireInventory(query);
        var metadata = queryCoordinator.prepare(query);
        return new ReportInventoryAnalysisVO("inventory", "CURRENT_STOCK_BY_STOCK_IN_MONTH",
                mapper.inventoryOverview(query), mapper.inventoryMonthly(query),
                mapper.inventoryWarehouses(query), metadata.dataAsOf());
    }

    @Transactional(readOnly = true)
    public ReportDeliveryAnalysisVO delivery(ReportQuery query) {
        queryPolicy.requireDelivery(query);
        var metadata = queryCoordinator.prepare(query);
        return new ReportDeliveryAnalysisVO("delivery", mapper.deliveryOverview(query),
                mapper.deliveryMonthly(query), mapper.deliveryWarehouses(query), metadata.dataAsOf());
    }
}
