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

    private SettleReceiveSheetWriter() {
    }

    static void write(Workbook workbook, SettleDetailVO detail) {
        Sheet sheet = workbook.createSheet("收款流水");
        writeSummary(sheet, detail);
        writeHeader(sheet, headerStyle(workbook));
        writeRows(sheet, detail.getReceives());
        autosize(sheet, 10);
    }

    private static void writeSummary(Sheet sheet, SettleDetailVO detail) {
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("收款流水");
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("结算单号");
        row.createCell(1).setCellValue(text(detail.getOrder().getSettleNo()));
        row.createCell(3).setCellValue("已收金额");
        row.createCell(4).setCellValue(text(detail.getOrder().getReceivedAmount()));
        row.createCell(6).setCellValue("未收金额");
        row.createCell(7).setCellValue(text(detail.getOrder().getUnreceivedAmount()));
    }

    private static void writeHeader(Sheet sheet, CellStyle style) {
        Row row = sheet.createRow(3);
        String[] labels = {"序号", "收款时间", "收款金额", "收款方式", "流水号", "经办人", "状态", "备注", "撤销人", "撤销原因"};
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
            row.createCell(0).setCellValue(index++);
            row.createCell(1).setCellValue(text(record.getReceiveDate()));
            row.createCell(2).setCellValue(text(record.getReceiveAmount()));
            row.createCell(3).setCellValue(payMethodText(record.getPayMethod()));
            row.createCell(4).setCellValue(text(record.getPayNo()));
            row.createCell(5).setCellValue(text(record.getOperator()));
            row.createCell(6).setCellValue(statusText(record.getRecordStatus()));
            row.createCell(7).setCellValue(text(record.getRemark()));
            row.createCell(8).setCellValue(text(record.getCancelBy()));
            row.createCell(9).setCellValue(text(record.getCancelReason()));
        }
    }

    private static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
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
        if (value == null || value == 1) {
            return "有效";
        }
        if (value == 2) {
            return "已撤销";
        }
        return String.valueOf(value);
    }

    private static void autosize(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 10000));
        }
    }

    private static String text(Object value) {
        return value == null ? "-" : value.toString();
    }

    private static String text(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }
}
