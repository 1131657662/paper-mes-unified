package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettleDetailVO;
import com.paper.mes.settle.dto.SettleFeeLineVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.math.BigDecimal;
import java.util.List;

final class SettleFeeSourceSheetWriter {

    private static final int COLUMN_COUNT = 17;

    private SettleFeeSourceSheetWriter() {
    }

    static void write(Workbook workbook, SettleDetailVO detail) {
        Sheet sheet = workbook.createSheet("费用来源");
        writeHeader(sheet, headerStyle(workbook));
        writeRows(sheet, detail.getPrintLines());
        autosize(sheet);
    }

    private static void writeHeader(Sheet sheet, CellStyle style) {
        Row row = sheet.createRow(0);
        String[] labels = {
                "序号", "加工单", "原纸", "费用类型", "阶段", "来源", "产出", "计费数量",
                "单位", "单价", "标准金额", "计价调整", "不含税金额", "税点", "税额", "含税/应收金额",
                "调整原因", "计算说明"
        };
        for (int i = 0; i < labels.length; i++) {
            row.createCell(i).setCellValue(labels[i]);
            row.getCell(i).setCellStyle(style);
        }
    }

    private static void writeRows(Sheet sheet, List<SettlePrintLineVO> lines) {
        int rowIndex = 1;
        int index = 1;
        if (lines == null) {
            return;
        }
        for (SettlePrintLineVO line : lines) {
            if (line.getFeeLines() == null || line.getFeeLines().isEmpty()) {
                continue;
            }
            for (SettleFeeLineVO feeLine : line.getFeeLines()) {
                writeRow(sheet.createRow(rowIndex++), index++, line, feeLine);
            }
        }
    }

    private static void writeRow(Row row, int index, SettlePrintLineVO line, SettleFeeLineVO feeLine) {
        row.createCell(0).setCellValue(index);
        row.createCell(1).setCellValue(text(line.getOrderNo()));
        row.createCell(2).setCellValue(text(line.getOriginalLabel()));
        row.createCell(3).setCellValue(text(feeLine.getFeeName()));
        row.createCell(4).setCellValue(stageText(feeLine.getStageLevel()));
        row.createCell(5).setCellValue(text(feeLine.getSourceText()));
        row.createCell(6).setCellValue(text(feeLine.getOutputText()));
        row.createCell(7).setCellValue(text(feeLine.getQuantity()));
        row.createCell(8).setCellValue(text(feeLine.getQuantityUnit()));
        row.createCell(9).setCellValue(text(feeLine.getUnitPrice()));
        row.createCell(10).setCellValue(text(feeLine.getStandardAmount()));
        row.createCell(11).setCellValue(text(feeLine.getPricingAdjustmentAmount()));
        row.createCell(12).setCellValue(text(feeLine.getAmountNoTax()));
        row.createCell(13).setCellValue(text(feeLine.getTaxRate()));
        row.createCell(14).setCellValue(text(feeLine.getTaxAmount()));
        row.createCell(15).setCellValue(text(feeLine.getAmountTax()));
        row.createCell(16).setCellValue(text(feeLine.getPricingAdjustmentReason()));
        row.createCell(17).setCellValue(text(feeLine.getFormulaText()));
    }

    private static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static void autosize(Sheet sheet) {
        for (int i = 0; i <= COLUMN_COUNT; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 16000));
        }
    }

    private static String stageText(Integer stageLevel) {
        return stageLevel == null ? "-" : "第" + stageLevel + "道";
    }

    private static String text(Object value) {
        return value == null ? "-" : value.toString();
    }

    private static String text(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }
}
