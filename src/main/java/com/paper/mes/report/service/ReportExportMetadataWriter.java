package com.paper.mes.report.service;

import com.paper.mes.report.dto.ReportQueryExecutionMetaVO;
import com.paper.mes.report.dto.ReportExportAuditMetadata;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Map;

final class ReportExportMetadataWriter {
    private ReportExportMetadataWriter() {
    }

    static void write(Workbook workbook, ReportQueryExecutionMetaVO metadata) {
        if (metadata == null) return;
        Sheet sheet = workbook.createSheet("数据口径");
        CellStyle titleStyle = titleStyle(workbook);
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("统计报表数据口径");
        title.getCell(0).setCellStyle(titleStyle);
        row(sheet, 2, "查询 ID", metadata.queryId());
        row(sheet, 3, "查询哈希", metadata.queryHash());
        row(sheet, 4, "指标发布包", metadata.metricReleaseUuid());
        row(sheet, 5, "数据截至", String.valueOf(metadata.dataAsOf()));
        row(sheet, 6, "源数据水位", String.valueOf(metadata.sourceWatermark()));
        row(sheet, 7, "一致性模式", metadata.consistencyMode());
        row(sheet, 8, "覆盖范围", metadata.coverage());
        int index = 10;
        for (Map.Entry<String, String> metric : metadata.metricVersionMap().entrySet()) {
            row(sheet, index++, "指标版本：" + metric.getKey(), metric.getValue());
        }
        sheet.setColumnWidth(0, 7200);
        sheet.setColumnWidth(1, 16000);
    }

    static void write(Workbook workbook, ReportExportAuditMetadata metadata) {
        if (metadata == null) return;
        Sheet sheet = workbook.createSheet("数据口径");
        CellStyle titleStyle = titleStyle(workbook);
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("统计报表数据口径");
        title.getCell(0).setCellStyle(titleStyle);
        row(sheet, 2, "来源页面", metadata.reportPath());
        row(sheet, 3, "查询快照 UUID", metadata.querySnapshotUuid());
        row(sheet, 4, "提交数据时点", String.valueOf(metadata.submissionDataAsOf()));
        row(sheet, 5, "执行数据时点", String.valueOf(metadata.executionDataAsOf()));
        row(sheet, 6, "指标发布包", metadata.metricReleaseUuid());
        int index = 8;
        for (Map.Entry<String, String> metric : metadata.metricVersionMap().entrySet()) {
            row(sheet, index++, "指标版本：" + metric.getKey(), metric.getValue());
        }
        sheet.setColumnWidth(0, 7200);
        sheet.setColumnWidth(1, 16000);
    }

    private static void row(Sheet sheet, int index, String label, String value) {
        Row row = sheet.createRow(index);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value == null ? "-" : value);
    }

    private static CellStyle titleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        var font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }
}
