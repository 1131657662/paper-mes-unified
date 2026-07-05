package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettleDetailVO;
import com.paper.mes.settle.entity.ReceiveRecord;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.math.BigDecimal;
import java.util.List;

final class SettleReceiveSheetWriter {

    private static final int COLUMN_COUNT = 14;

    private SettleReceiveSheetWriter() {
    }

    static void write(Workbook workbook, SettleDetailVO detail) {
        Sheet sheet = workbook.createSheet("收款流水");
        writeSummary(sheet, detail);
        writeHeader(sheet, headerStyle(workbook));
        writeRows(sheet, detail.getReceives());
        autosize(sheet);
    }

    private static void writeSummary(Sheet sheet, SettleDetailVO detail) {
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("收款流水");
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("结算单号");
        row.createCell(1).setCellValue(text(detail.getOrder().getSettleNo()));
        row.createCell(3).setCellValue("已结清");
        row.createCell(4).setCellValue(text(detail.getOrder().getReceivedAmount()));
        row.createCell(5).setCellValue("现金实收");
        row.createCell(6).setCellValue(text(detail.getOrder().getCashReceivedAmount()));
        row.createCell(7).setCellValue("废纸抵扣");
        row.createCell(8).setCellValue(text(detail.getOrder().getScrapOffsetAmount()));
        row.createCell(10).setCellValue("未收金额");
        row.createCell(11).setCellValue(text(detail.getOrder().getUnreceivedAmount()));
    }

    private static void writeHeader(Sheet sheet, CellStyle style) {
        Row row = sheet.createRow(3);
        String[] labels = {
                "序号", "收款时间", "类型", "本次结清", "现金实收", "废纸抵扣", "废纸重量kg", "折算单价",
                "收款方式", "流水号", "经办人", "状态", "备注", "撤销信息"
        };
        for (int i = 0; i < labels.length; i++) {
            row.createCell(i).setCellValue(labels[i]);
            row.getCell(i).setCellStyle(style);
        }
    }

    private static void writeRows(Sheet sheet, List<ReceiveRecord> receives) {
        if (receives == null || receives.isEmpty()) {
            sheet.createRow(4).createCell(0).setCellValue("暂无收款流水");
            return;
        }
        int rowIndex = 4;
        int index = 1;
        for (ReceiveRecord record : receives) {
            Row row = sheet.createRow(rowIndex++);
            writeRow(row, index++, record);
        }
    }

    private static void writeRow(Row row, int index, ReceiveRecord record) {
        row.createCell(0).setCellValue(index);
        row.createCell(1).setCellValue(text(record.getReceiveDate()));
        row.createCell(2).setCellValue(receiveTypeText(record.getReceiveType()));
        row.createCell(3).setCellValue(text(record.getReceiveAmount()));
        row.createCell(4).setCellValue(text(record.getCashAmount()));
        row.createCell(5).setCellValue(text(record.getScrapOffsetAmount()));
        row.createCell(6).setCellValue(text(record.getScrapWeight()));
        row.createCell(7).setCellValue(text(record.getScrapUnitPrice()));
        row.createCell(8).setCellValue(payMethodText(record.getPayMethod()));
        row.createCell(9).setCellValue(text(record.getPayNo()));
        row.createCell(10).setCellValue(text(record.getOperator()));
        row.createCell(11).setCellValue(statusText(record.getRecordStatus()));
        row.createCell(12).setCellValue(text(record.getRemark()));
        row.createCell(13).setCellValue(cancelText(record));
    }

    private static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static String receiveTypeText(Integer value) {
        if (value == null || value == 1) {
            return "普通收款";
        }
        if (value == 2) {
            return "废纸抵扣";
        }
        if (value == 3) {
            return "混合收款";
        }
        return String.valueOf(value);
    }

    private static String payMethodText(Integer value) {
        if (value == null) {
            return "-";
        }
        return switch (value) {
            case 1 -> "现金";
            case 2 -> "转账";
            case 3 -> "微信";
            case 4 -> "支付宝";
            default -> String.valueOf(value);
        };
    }

    private static String statusText(Integer value) {
        return value != null && value == 2 ? "已撤销" : "有效";
    }

    private static String cancelText(ReceiveRecord record) {
        if (record.getRecordStatus() == null || record.getRecordStatus() != 2) {
            return "-";
        }
        return text(record.getCancelBy()) + " / " + text(record.getCancelReason());
    }

    private static void autosize(Sheet sheet) {
        for (int i = 0; i < COLUMN_COUNT; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 12000));
        }
    }

    private static String text(Object value) {
        return value == null ? "-" : value.toString();
    }

    private static String text(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }
}
