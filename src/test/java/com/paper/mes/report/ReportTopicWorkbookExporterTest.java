package com.paper.mes.report;

import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportExportAuditMetadata;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.mapper.ReportMapper;
import com.paper.mes.report.service.ReportTopicWorkbookExporter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportTopicWorkbookExporterTest {
    @Test
    void build_whenProductionPath_usesProductionSheets() throws Exception {
        ReportMapper mapper = mapper();

        try (var workbook = new ReportTopicWorkbookExporter(mapper)
                .build("/reports/production", new ReportQuery(), metadata())) {
            assertNotNull(workbook.getSheet("生产总览"));
            assertNotNull(workbook.getSheet("工艺结构"));
            assertNotNull(workbook.getSheet("机台负荷"));
            assertNull(workbook.getSheet("加工单明细"));
            workbook.write(OutputStream.nullOutputStream());
        }
    }

    @Test
    void build_whenQualityLossPath_usesLossSheets() throws Exception {
        ReportMapper mapper = mapper();
        when(mapper.lossLeaderRows(any(), org.mockito.ArgumentMatchers.eq(10)))
                .thenReturn(List.of(new ReportDetailVO()));

        try (var workbook = new ReportTopicWorkbookExporter(mapper)
                .build("/reports/quality-loss", new ReportQuery(), metadata())) {
            assertNotNull(workbook.getSheet("损耗总览"));
            assertNotNull(workbook.getSheet("纸品损耗"));
            assertNotNull(workbook.getSheet("高损耗订单"));
            assertNull(workbook.getSheet("加工单明细"));
            workbook.write(OutputStream.nullOutputStream());
        }
    }

    private ReportMapper mapper() {
        ReportMapper mapper = mock(ReportMapper.class);
        when(mapper.overview(any())).thenReturn(new ReportOverviewVO());
        when(mapper.dimensionSummary(any(), anyString())).thenReturn(List.of(new ReportDimensionVO()));
        return mapper;
    }

    private ReportExportAuditMetadata metadata() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 24, 10, 0);
        return new ReportExportAuditMetadata("/reports/production", "snapshot", now, now,
                "release", Map.of());
    }
}
