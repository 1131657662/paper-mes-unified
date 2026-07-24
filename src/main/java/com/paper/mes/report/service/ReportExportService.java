package com.paper.mes.report.service;

import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportQueryExecutionMetaVO;
import com.paper.mes.report.dto.ReportExportAuditMetadata;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ReportExportService {
    public SXSSFWorkbook buildWorkbook(ReportOverviewVO overview,
                                       List<ReportDimensionVO> dimensions,
                                       Iterable<ReportDetailVO> details,
                                       String dimension) {
        return buildWorkbook(overview, dimensions, details, dimension, null);
    }

    public SXSSFWorkbook buildWorkbook(ReportOverviewVO overview,
                                       List<ReportDimensionVO> dimensions,
                                       Iterable<ReportDetailVO> details,
                                       String dimension,
                                       ReportQueryExecutionMetaVO metadata) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        CellStyle titleStyle = titleStyle(workbook);
        CellStyle headerStyle = headerStyle(workbook);
        SXSSFSheet overviewSheet = workbook.createSheet("汇总");
        SXSSFSheet dimensionSheet = workbook.createSheet("维度汇总");
        overviewSheet.trackAllColumnsForAutoSizing();
        dimensionSheet.trackAllColumnsForAutoSizing();
        writeOverview(overviewSheet, overview, titleStyle);
        writeDimensions(dimensionSheet, dimensions, dimension, headerStyle);
        writeDetails(workbook.createSheet("加工单明细"), details, headerStyle);
        ReportExportMetadataWriter.write(workbook, metadata);
        return workbook;
    }

    public SXSSFWorkbook buildAuditedWorkbook(ReportOverviewVO overview,
                                              List<ReportDimensionVO> dimensions,
                                              Iterable<ReportDetailVO> details,
                                              String dimension,
                                              ReportExportAuditMetadata metadata) {
        SXSSFWorkbook workbook = buildWorkbook(overview, dimensions, details, dimension);
        ReportExportMetadataWriter.write(workbook, metadata);
        return workbook;
    }

    private void writeOverview(Sheet sheet, ReportOverviewVO overview, CellStyle titleStyle) {
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("统计报表汇总");
        title.getCell(0).setCellStyle(titleStyle);
        row(sheet, 2, "加工单数", overview.getOrderCount(), "原卷数", overview.getOriginalRollCount());
        row(sheet, 3, "成品卷数", overview.getFinishRollCount(), "刀数", overview.getKnifeCount());
        row(sheet, 4, "原纸吨位", ton(overview.getOriginalWeight()), "成品吨位", ton(overview.getFinishWeight()));
        row(sheet, 5, "损耗吨位", ton(overview.getLossWeight()), "损耗率%", overview.getLossRatio());
        row(sheet, 6, "锯纸费", overview.getSawAmount(), "复卷费", overview.getRewindAmount());
        row(sheet, 7, "加工费", overview.getProcessAmount(), "附加费", overview.getExtraAmount());
        row(sheet, 8, "应收合计", overview.getTotalAmount(), "已结算应收", overview.getSettledAmount());
        row(sheet, 9, "待结算应收", overview.getPendingSettleAmount(), "已结清", overview.getReceivedAmount());
        row(sheet, 10, "实际到账", overview.getCashReceivedAmount(), "废纸抵扣", overview.getScrapOffsetAmount());
        row(sheet, 11, "已结算未收", overview.getUnreceivedAmount(), "", "");
        autosize(sheet, 5);
    }

    private void writeDimensions(Sheet sheet, List<ReportDimensionVO> rows, String dimension, CellStyle style) {
        header(sheet, style, "维度", "加工单", "原卷", "成品", "原纸吨位", "成品吨位", "损耗吨位",
                "损耗率%", "刀数", "锯纸费", "复卷费", "加工费", "附加费", "应收合计",
                "已结算应收", "待结算应收", "已结清", "实际到账", "废纸抵扣", "已结算未收");
        int index = 1;
        for (ReportDimensionVO item : rows) {
            Row row = sheet.createRow(index++);
            row.createCell(0).setCellValue(ReportExportTexts.label(item, dimension));
            row.createCell(1).setCellValue(num(item.getOrderCount()));
            row.createCell(2).setCellValue(num(item.getOriginalRollCount()));
            row.createCell(3).setCellValue(num(item.getFinishRollCount()));
            row.createCell(4).setCellValue(numTon(item.getOriginalWeight()));
            row.createCell(5).setCellValue(numTon(item.getFinishWeight()));
            row.createCell(6).setCellValue(numTon(item.getLossWeight()));
            row.createCell(7).setCellValue(num(item.getLossRatio()));
            row.createCell(8).setCellValue(num(item.getKnifeCount()));
            row.createCell(9).setCellValue(num(item.getSawAmount()));
            row.createCell(10).setCellValue(num(item.getRewindAmount()));
            row.createCell(11).setCellValue(num(item.getProcessAmount()));
            row.createCell(12).setCellValue(num(item.getExtraAmount()));
            row.createCell(13).setCellValue(num(item.getTotalAmount()));
            row.createCell(14).setCellValue(num(item.getSettledAmount()));
            row.createCell(15).setCellValue(num(item.getPendingSettleAmount()));
            row.createCell(16).setCellValue(num(item.getReceivedAmount()));
            row.createCell(17).setCellValue(num(item.getCashReceivedAmount()));
            row.createCell(18).setCellValue(num(item.getScrapOffsetAmount()));
            row.createCell(19).setCellValue(num(item.getUnreceivedAmount()));
        }
        autosize(sheet, 20);
    }

    private void writeDetails(Sheet sheet, Iterable<ReportDetailVO> rows, CellStyle style) {
        header(sheet, style, "加工单号", "制单日期", "客户", "纸品", "工艺", "状态", "结算",
                "开票", "原卷", "成品", "原纸吨位", "成品吨位", "损耗吨位", "损耗率%", "刀数",
                "锯纸费", "复卷费", "加工费", "附加费", "应收合计", "已结算应收", "待结算应收",
                "已结清", "实际到账", "废纸抵扣", "已结算未收");
        int index = 1;
        for (ReportDetailVO item : rows) {
            writeDetailRow(sheet.createRow(index++), item);
        }
        setDetailColumnWidths(sheet);
    }

    private void setDetailColumnWidths(Sheet sheet) {
        for (int index = 0; index < 26; index++) {
            sheet.setColumnWidth(index, index < 8 ? 4200 : 3200);
        }
    }

    private void writeDetailRow(Row row, ReportDetailVO item) {
        Object[] values = {
                item.getOrderNo(), item.getOrderDate(), item.getCustomerName(), item.getPaperSummary(),
                item.getProcessSummary(), ReportExportTexts.statusText(item.getOrderStatus()),
                ReportExportTexts.settleText(item.getSettleType()), ReportExportTexts.invoiceText(item.getIsInvoice()),
                num(item.getOriginalRollCount()), num(item.getFinishRollCount()),
                numTon(item.getOriginalWeight()), numTon(item.getFinishWeight()), numTon(item.getLossWeight()),
                num(item.getLossRatio()), num(item.getKnifeCount()), num(item.getSawAmount()),
                num(item.getRewindAmount()), num(item.getProcessAmount()), num(item.getExtraAmount()),
                num(item.getTotalAmount()), num(item.getSettledAmount()), num(item.getPendingSettleAmount()),
                num(item.getReceivedAmount()), num(item.getCashReceivedAmount()), num(item.getScrapOffsetAmount()),
                num(item.getUnreceivedAmount())
        };
        for (int i = 0; i < values.length; i++) {
            cell(row, i, values[i]);
        }
    }

    private void header(Sheet sheet, CellStyle style, String... labels) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < labels.length; i++) {
            row.createCell(i).setCellValue(labels[i]);
            row.getCell(i).setCellStyle(style);
        }
    }

    private void row(Sheet sheet, int rowIndex, String k1, Object v1, String k2, Object v2) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(k1);
        cell(row, 1, v1);
        row.createCell(3).setCellValue(k2);
        cell(row, 4, v2);
    }

    private void cell(Row row, int index, Object value) {
        if (value instanceof Number number) {
            row.createCell(index).setCellValue(number.doubleValue());
            return;
        }
        row.createCell(index).setCellValue(text(value));
    }

    private CellStyle titleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void autosize(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 12000));
        }
    }

    private double num(BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }

    private BigDecimal ton(BigDecimal kg) {
        return kg == null ? BigDecimal.ZERO : kg.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
    }

    private double numTon(BigDecimal kg) {
        return ton(kg).doubleValue();
    }

    private double num(Long value) {
        return value == null ? 0 : value.doubleValue();
    }

    private String text(Object value) {
        return value == null ? "-" : value.toString();
    }
}
