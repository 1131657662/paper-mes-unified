package com.paper.mes.report;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportDetailQuery;
import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportQueryExecutionMetaVO;
import com.paper.mes.report.mapper.ReportMapper;
import com.paper.mes.report.service.ReportExportService;
import com.paper.mes.report.service.ReportExportConsistencyGuard;
import com.paper.mes.report.service.ReportOperationalWorkbookExporter;
import com.paper.mes.report.service.ReportQueryCoordinator;
import com.paper.mes.report.service.ReportTopicWorkbookExporter;
import com.paper.mes.report.service.ReportWorkbookExportCoordinator;
import com.paper.mes.report.service.impl.ReportServiceImpl;
import org.apache.ibatis.cursor.Cursor;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceImplTest {

    @Test
    void productionAnalysis_returnsProductionSpecificBreakdowns() {
        ReportMapper mapper = mock(ReportMapper.class);
        ReportQueryCoordinator coordinator = coordinator();
        ReportDimensionVO row = new ReportDimensionVO();
        when(mapper.overview(org.mockito.ArgumentMatchers.any())).thenReturn(new ReportOverviewVO());
        when(mapper.dimensionSummary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(row));

        var result = service(mapper, coordinator).productionAnalysis(new ReportQuery());

        assertEquals(1, result.monthlyTrend().size());
        assertEquals("query", result.execution().queryId());
        verify(mapper).dimensionSummary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("process"));
        verify(mapper).dimensionSummary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("machine"));
    }

    @Test
    void pageAnalysis_returnsAllSectionsWithOneExecutionContext() {
        ReportMapper mapper = mock(ReportMapper.class);
        when(mapper.overview(org.mockito.ArgumentMatchers.any())).thenReturn(new ReportOverviewVO());
        when(mapper.dimensionSummary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(new ReportDimensionVO()));
        when(mapper.detailRows(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of(new ReportDetailVO()));
        ReportDetailQuery query = new ReportDetailQuery();
        query.setDimension("machine");

        var result = service(mapper, coordinator()).pageAnalysis(query);

        assertEquals("query", result.execution().queryId());
        assertEquals(1, result.currentBreakdown().size());
        assertEquals(1, result.details().getRecords().size());
        verify(mapper).dimensionSummary(query, "machine");
        verify(mapper).dimensionSummary(query, "month");
        verify(mapper).dimensionSummary(query, "customer");
        verify(mapper).dimensionSummary(query, "paper");
    }

    @Test
    void qualityLossAnalysis_returnsDatabaseRankedLossLeaders() {
        ReportMapper mapper = mock(ReportMapper.class);
        when(mapper.overview(org.mockito.ArgumentMatchers.any())).thenReturn(new ReportOverviewVO());
        when(mapper.dimensionSummary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of());
        when(mapper.lossLeaderRows(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(10)))
                .thenReturn(List.of(new ReportDetailVO()));

        var result = service(mapper, coordinator()).qualityLossAnalysis(new ReportQuery());

        assertEquals(1, result.lossLeaders().size());
        verify(mapper).dimensionSummary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("paper"));
    }

    @Test
    void detailRows_whenSecondPageRequested_usesBoundedOffsetAndSize() {
        ReportMapper mapper = mock(ReportMapper.class);
        when(mapper.detailCount(org.mockito.ArgumentMatchers.any())).thenReturn(1_250L);
        when(mapper.detailRows(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(List.of(new ReportDetailVO()));
        ReportServiceImpl service = service(mapper);
        ReportDetailQuery query = new ReportDetailQuery();
        query.setCurrent(2);
        query.setSize(20);

        var result = service.detailRows(query);

        assertEquals(1_250L, result.getTotal());
        assertEquals(2L, result.getCurrent());
        assertEquals(20L, result.getSize());
        assertEquals(1, result.getRecords().size());
        verify(mapper).detailRows(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.eq(20L));
    }

    @Test
    void exportWorkbook_whenResultExceedsCapacity_rejectsBeforeOpeningCursor() throws Exception {
        ReportMapper mapper = mock(ReportMapper.class);
        when(mapper.detailCount(org.mockito.ArgumentMatchers.any())).thenReturn(100_001L);
        ReportServiceImpl service = service(mapper);
        Path target = Files.createTempFile("report-capacity", ".xlsx");

        try {
            assertThrows(BusinessException.class,
                    () -> service.exportWorkbook(new ReportQuery(), target));
        } finally {
            Files.deleteIfExists(target);
        }
    }

    @Test
    void exportWorkbook_whenWithinCapacity_streamsCursorIntoWorkbook() throws Exception {
        ReportMapper mapper = mock(ReportMapper.class);
        @SuppressWarnings("unchecked") Cursor<ReportDetailVO> cursor = mock(Cursor.class);
        when(mapper.detailCount(org.mockito.ArgumentMatchers.any())).thenReturn(1L);
        when(mapper.overview(org.mockito.ArgumentMatchers.any())).thenReturn(new ReportOverviewVO());
        when(mapper.dimensionSummary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of());
        when(mapper.detailCursor(org.mockito.ArgumentMatchers.any())).thenReturn(cursor);
        when(cursor.iterator()).thenReturn(List.of(new ReportDetailVO()).iterator());
        ReportServiceImpl service = service(mapper);
        Path target = Files.createTempFile("report-export", ".xlsx");

        try {
            service.exportWorkbook(new ReportQuery(), target);

            assertTrue(Files.size(target) > 0);
            verify(cursor).close();
        } finally {
            Files.deleteIfExists(target);
        }
    }

    private ReportServiceImpl service(ReportMapper mapper) {
        return service(mapper, mock(ReportQueryCoordinator.class));
    }

    private ReportServiceImpl service(ReportMapper mapper, ReportQueryCoordinator coordinator) {
        var workbookCoordinator = new ReportWorkbookExportCoordinator(mapper, new ReportExportService(),
                mock(ReportTopicWorkbookExporter.class), mock(ReportOperationalWorkbookExporter.class));
        return new ReportServiceImpl(mapper, coordinator, mock(ReportExportConsistencyGuard.class),
                workbookCoordinator);
    }

    private ReportQueryCoordinator coordinator() {
        ReportQueryCoordinator coordinator = mock(ReportQueryCoordinator.class);
        LocalDateTime now = LocalDateTime.of(2026, 7, 21, 11, 0);
        when(coordinator.prepare(org.mockito.ArgumentMatchers.any())).thenReturn(
                new ReportQueryExecutionMetaVO("query", "hash", "release", Map.of(), now, now,
                        "LIVE_DB_READ", "LIVE_ONLY", List.of(), Map.of()));
        return coordinator;
    }
}
