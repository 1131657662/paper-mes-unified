package com.paper.mes.report;

import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.service.ReportExportService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
            assertEquals("应收合计", details.getRow(0).getCell(19).getStringCellValue());
            assertEquals("有效已收", details.getRow(0).getCell(22).getStringCellValue());
        }
    }
}
