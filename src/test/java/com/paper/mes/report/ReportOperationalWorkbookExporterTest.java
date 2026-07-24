package com.paper.mes.report;

import com.paper.mes.report.dto.ReportExportAuditMetadata;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportSettlementAnalysisVO;
import com.paper.mes.report.mapper.ReportOperationalMapper;
import com.paper.mes.report.service.ReportOperationalQueryPolicy;
import com.paper.mes.report.service.ReportOperationalWorkbookExporter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportOperationalWorkbookExporterTest {
    @Test
    void build_whenSettlementPath_usesSettlementQueriesAndSheets() throws Exception {
        ReportOperationalMapper mapper = mock(ReportOperationalMapper.class);
        when(mapper.settlementOverview(any())).thenReturn(new ReportSettlementAnalysisVO.Overview());
        when(mapper.settlementMonthly(any())).thenReturn(List.of());
        when(mapper.settlementCustomers(any())).thenReturn(List.of());
        var exporter = new ReportOperationalWorkbookExporter(mapper, new ReportOperationalQueryPolicy());

        try (var workbook = exporter.build("/reports/settlement", new ReportQuery(), metadata())) {
            assertNotNull(workbook.getSheet("结算总览"));
            assertNotNull(workbook.getSheet("月度趋势"));
            assertNotNull(workbook.getSheet("客户应收"));
            assertNotNull(workbook.getSheet("数据口径"));
            assertNull(workbook.getSheet("加工单明细"));
            workbook.write(OutputStream.nullOutputStream());
        }
    }

    private ReportExportAuditMetadata metadata() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 24, 10, 0);
        return new ReportExportAuditMetadata("/reports/settlement", "snapshot", now, now,
                "release", Map.of());
    }
}
