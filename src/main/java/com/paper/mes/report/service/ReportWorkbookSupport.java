package com.paper.mes.report.service;

import com.paper.mes.report.dto.ReportExportAuditMetadata;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class ReportWorkbookSupport {
    private ReportWorkbookSupport() {
    }

    static SXSSFWorkbook workbook() {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        return workbook;
    }

    static void summary(SXSSFWorkbook workbook, String sheetName, String title, Object[][] metrics) {
        Sheet sheet = trackedSheet(workbook, sheetName);
        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue(title);
        titleRow.getCell(0).setCellStyle(titleStyle(workbook));
        for (int index = 0; index < metrics.length; index++) {
            writeRow(sheet.createRow(index + 2), metrics[index]);
        }
        autosize(sheet, 4);
    }

    static void table(SXSSFWorkbook workbook, String sheetName, String[] headers,
                      Iterable<Object[]> rows) {
        Sheet sheet = trackedSheet(workbook, sheetName);
        writeHeader(sheet.createRow(0), headers, headerStyle(workbook));
        int index = 1;
        for (Object[] values : rows) {
            writeRow(sheet.createRow(index++), values);
        }
        autosize(sheet, headers.length);
    }

    static void metadata(SXSSFWorkbook workbook, ReportExportAuditMetadata metadata) {
        ReportExportMetadataWriter.write(workbook, metadata);
    }

    static double number(Number value) {
        return value == null ? 0 : value.doubleValue();
    }

    static double tons(BigDecimal kilograms) {
        if (kilograms == null) return 0;
        return kilograms.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP).doubleValue();
    }

    private static SXSSFSheet trackedSheet(SXSSFWorkbook workbook, String name) {
        SXSSFSheet sheet = workbook.createSheet(name);
        sheet.trackAllColumnsForAutoSizing();
        return sheet;
    }

    private static void writeHeader(Row row, String[] headers, CellStyle style) {
        for (int index = 0; index < headers.length; index++) {
            row.createCell(index).setCellValue(headers[index]);
            row.getCell(index).setCellStyle(style);
        }
    }

    private static void writeRow(Row row, Object[] values) {
        for (int index = 0; index < values.length; index++) {
            writeCell(row, index, values[index]);
        }
    }

    private static void writeCell(Row row, int index, Object value) {
        if (value instanceof Number number) {
            row.createCell(index).setCellValue(number.doubleValue());
            return;
        }
        row.createCell(index).setCellValue(value == null ? "-" : value.toString());
    }

    private static CellStyle titleStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        var font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private static CellStyle headerStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        var font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static void autosize(Sheet sheet, int columns) {
        for (int index = 0; index < columns; index++) {
            sheet.autoSizeColumn(index);
            sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 512, 12000));
        }
    }
}
