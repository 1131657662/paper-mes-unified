package com.paper.mes.report;

import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.service.ReportExportService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportExportServiceTest {

    @Test
    void buildWorkbook_whenWritingMoneyHeaders_usesReceivableVocabulary() throws IOException {
        ReportExportService service = new ReportExportService();

        try (var workbook = service.buildWorkbook(new ReportOverviewVO(), List.of(), List.of(), "customer")) {
            var overview = workbook.getSheet("汇总");
            var dimension = workbook.getSheet("维度汇总");
            var details = workbook.getSheet("加工单明细");

            assertEquals("应收合计", overview.getRow(8).getCell(0).getStringCellValue());
            assertEquals("已结算应收", overview.getRow(8).getCell(3).getStringCellValue());
            assertEquals("待结算应收", overview.getRow(9).getCell(0).getStringCellValue());
            assertEquals("有效已收", overview.getRow(9).getCell(3).getStringCellValue());
            assertEquals("应收合计", dimension.getRow(0).getCell(13).getStringCellValue());
            assertEquals("有效已收", dimension.getRow(0).getCell(16).getStringCellValue());
            assertEquals("制单日期", details.getRow(0).getCell(1).getStringCellValue());
            assertEquals("应收合计", details.getRow(0).getCell(19).getStringCellValue());
            assertEquals("有效已收", details.getRow(0).getCell(22).getStringCellValue());
        }
    }

    @Test
    void buildWorkbook_whenWritingReportWeights_exportsTons() throws IOException {
        ReportExportService service = new ReportExportService();
        ReportOverviewVO overview = new ReportOverviewVO();
        overview.setOriginalWeight(new BigDecimal("1234.567"));
        overview.setFinishWeight(new BigDecimal("987.654"));
        overview.setLossWeight(new BigDecimal("12.345"));

        ReportDimensionVO dimensionRow = new ReportDimensionVO();
        dimensionRow.setDimensionName("测试客户");
        dimensionRow.setOriginalWeight(new BigDecimal("3255"));
        dimensionRow.setFinishWeight(new BigDecimal("2202"));
        dimensionRow.setLossWeight(new BigDecimal("53"));

        ReportDetailVO detailRow = new ReportDetailVO();
        detailRow.setOriginalWeight(new BigDecimal("6510"));
        detailRow.setFinishWeight(new BigDecimal("4404"));
        detailRow.setLossWeight(new BigDecimal("106"));

        try (var workbook = service.buildWorkbook(overview, List.of(dimensionRow), List.of(detailRow), "customer")) {
            var overviewSheet = workbook.getSheet("汇总");
            var dimensionSheet = workbook.getSheet("维度汇总");
            var detailSheet = workbook.getSheet("加工单明细");

            assertEquals("原纸吨位", overviewSheet.getRow(4).getCell(0).getStringCellValue());
            assertEquals("1.235", overviewSheet.getRow(4).getCell(1).getStringCellValue());
            assertEquals("损耗吨位", overviewSheet.getRow(5).getCell(0).getStringCellValue());
            assertEquals("0.012", overviewSheet.getRow(5).getCell(1).getStringCellValue());
            assertEquals("原纸吨位", dimensionSheet.getRow(0).getCell(4).getStringCellValue());
            assertEquals(3.255, dimensionSheet.getRow(1).getCell(4).getNumericCellValue(), 0.0001);
            assertEquals("原纸吨位", detailSheet.getRow(0).getCell(10).getStringCellValue());
            assertEquals(6.51, detailSheet.getRow(1).getCell(10).getNumericCellValue(), 0.0001);
        }
    }
}
