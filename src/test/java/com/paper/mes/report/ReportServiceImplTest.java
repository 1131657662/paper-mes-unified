package com.paper.mes.report;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.mapper.ReportMapper;
import com.paper.mes.report.service.ReportExportService;
import com.paper.mes.report.service.impl.ReportServiceImpl;
import org.apache.ibatis.cursor.Cursor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceImplTest {

    @Test
    void detailRows_whenResultExceedsDisplayLimit_returnsTruncationSummary() {
        ReportMapper mapper = mock(ReportMapper.class);
        when(mapper.detailCount(org.mockito.ArgumentMatchers.any())).thenReturn(1_250L);
        when(mapper.detailRows(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of(new ReportDetailVO()));
        ReportServiceImpl service = new ReportServiceImpl(mapper, new ReportExportService());

        var result = service.detailRows(new ReportQuery());

        assertEquals(1_250L, result.getTotal());
        assertEquals(1_000, result.getDisplayLimit());
        assertTrue(result.isTruncated());
        verify(mapper).detailRows(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1_000));
    }

    @Test
    void exportWorkbook_whenResultExceedsCapacity_rejectsBeforeOpeningCursor() {
        ReportMapper mapper = mock(ReportMapper.class);
        when(mapper.detailCount(org.mockito.ArgumentMatchers.any())).thenReturn(100_001L);
        ReportServiceImpl service = new ReportServiceImpl(mapper, new ReportExportService());

        assertThrows(BusinessException.class,
                () -> service.exportWorkbook(new ReportQuery(), new MockHttpServletResponse()));
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
        ReportServiceImpl service = new ReportServiceImpl(mapper, new ReportExportService());
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.exportWorkbook(new ReportQuery(), response);

        assertTrue(response.getContentAsByteArray().length > 0);
        verify(cursor).close();
    }
}
