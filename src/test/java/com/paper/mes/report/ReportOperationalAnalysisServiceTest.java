package com.paper.mes.report;

import com.paper.mes.report.dto.ReportInventoryAnalysisVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.mapper.ReportOperationalMapper;
import com.paper.mes.report.service.ReportOperationalAnalysisService;
import com.paper.mes.report.service.ReportOperationalQueryPolicy;
import com.paper.mes.report.service.ReportAmountVisibility;
import com.paper.mes.report.service.ReportQueryCoordinator;
import com.paper.mes.auth.permission.PermissionChecker;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportOperationalAnalysisServiceTest {

    @Test
    void settlement_usesSettlementFactsAndVersionedAsOf() {
        Fixture fixture = fixture();

        var result = fixture.service.settlement(fixture.query);

        assertEquals("settlement", result.topicCode());
        assertEquals(fixture.asOf, result.asOf());
        verify(fixture.mapper).settlementOverview(fixture.query);
        verify(fixture.mapper).settlementMonthly(fixture.query);
        verify(fixture.mapper).settlementCustomers(fixture.query);
        verify(fixture.coordinator).prepare(fixture.query, Set.of(
                "settlement_document_count", "settlement_pending_count", "settlement_partial_count",
                "overdue_document_count", "overdue_amount", "settled_amount", "received_amount",
                "unreceived_amount"));
    }

    @Test
    void inventory_declaresCurrentStockCohortTimeline() {
        Fixture fixture = fixture();
        when(fixture.mapper.inventoryOverview(any())).thenReturn(new ReportInventoryAnalysisVO.Overview());
        when(fixture.mapper.inventoryMonthly(any())).thenReturn(List.of());
        when(fixture.mapper.inventoryWarehouses(any())).thenReturn(List.of());

        var result = fixture.service.inventory(fixture.query);

        assertEquals("CURRENT_STOCK_BY_STOCK_IN_MONTH", result.timelineMode());
        verify(fixture.mapper).inventoryWarehouses(fixture.query);
        verify(fixture.coordinator).prepare(fixture.query, Set.of(
                "inventory_roll_count", "inventory_available_count", "inventory_locked_count",
                "inventory_exception_count", "inventory_weight_kg", "inventory_locked_weight_kg"));
    }

    private Fixture fixture() {
        ReportOperationalMapper mapper = mock(ReportOperationalMapper.class);
        ReportQueryCoordinator coordinator = mock(ReportQueryCoordinator.class);
        LocalDateTime asOf = LocalDateTime.of(2026, 7, 21, 12, 0);
        when(coordinator.prepare(any(), any())).thenReturn(new com.paper.mes.report.dto.ReportQueryExecutionMetaVO(
                "query", "hash", "release", Map.of(), asOf, asOf,
                "LIVE_DB_READ", "LIVE_ONLY", List.of(), Map.of()));
        return new Fixture(mapper, coordinator, new ReportOperationalAnalysisService(mapper, coordinator,
                new ReportOperationalQueryPolicy(), new ReportAmountVisibility(new PermissionChecker())),
                new ReportQuery(), asOf);
    }

    private record Fixture(ReportOperationalMapper mapper, ReportQueryCoordinator coordinator,
                           ReportOperationalAnalysisService service, ReportQuery query, LocalDateTime asOf) {
    }
}
