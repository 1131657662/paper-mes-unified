package com.paper.mes.report;

import com.paper.mes.report.dto.ReportInventoryAnalysisVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.mapper.ReportOperationalMapper;
import com.paper.mes.report.service.ReportOperationalAnalysisService;
import com.paper.mes.report.service.ReportOperationalQueryPolicy;
import com.paper.mes.report.service.ReportQueryCoordinator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    }

    private Fixture fixture() {
        ReportOperationalMapper mapper = mock(ReportOperationalMapper.class);
        ReportQueryCoordinator coordinator = mock(ReportQueryCoordinator.class);
        LocalDateTime asOf = LocalDateTime.of(2026, 7, 21, 12, 0);
        when(coordinator.prepare(any())).thenReturn(new com.paper.mes.report.dto.ReportQueryExecutionMetaVO(
                "query", "hash", "release", Map.of(), asOf, asOf,
                "LIVE_DB_READ", "LIVE_ONLY", List.of(), Map.of()));
        return new Fixture(mapper, new ReportOperationalAnalysisService(mapper, coordinator,
                new ReportOperationalQueryPolicy()),
                new ReportQuery(), asOf);
    }

    private record Fixture(ReportOperationalMapper mapper, ReportOperationalAnalysisService service,
                           ReportQuery query, LocalDateTime asOf) {
    }
}
