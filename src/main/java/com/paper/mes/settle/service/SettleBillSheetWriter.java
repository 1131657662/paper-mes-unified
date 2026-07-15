package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettleDetailVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import com.paper.mes.settle.entity.SettleOrder;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;

import static com.paper.mes.settle.service.SettleBillLineText.feeBasis;
import static com.paper.mes.settle.service.SettleBillLineText.finishResult;
import static com.paper.mes.settle.service.SettleBillLineText.originalSpec;
import static com.paper.mes.settle.service.SettleBillLineText.originalText;
import static com.paper.mes.settle.service.SettleBillLineText.processNames;
import static com.paper.mes.settle.service.SettleBillLineText.text;
import static com.paper.mes.settle.service.SettleBillLineText.trimText;
import static com.paper.mes.settle.service.SettleBillLineText.weightText;

final class SettleBillSheetWriter {

    private static final int COLUMN_COUNT = 8;

    private SettleBillSheetWriter() {
    }

    static void write(Workbook workbook, SettleDetailVO detail) {
        Sheet sheet = workbook.createSheet("结算单");
        CellStyle boldStyle = SettleSheetStyles.boldStyle(workbook);
        writeSummary(sheet, detail.getOrder(), SettleSheetStyles.titleStyle(workbook));
        writeHeader(sheet, boldStyle);
        writeLines(sheet, detail.getPrintLines(), boldStyle);
        autosize(sheet);
    }

    private static void writeSummary(Sheet sheet, SettleOrder order, CellStyle titleStyle) {
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("客户结算单");
        title.getCell(0).setCellStyle(titleStyle);
        summaryRow(sheet, 1, "结算单号", order.getSettleNo(), "客户", order.getCustomerName());
        summaryRow(sheet, 2, "结算日期", text(order.getSettleDate()), "账期", periodText(order));
        summaryRow(sheet, 3, "开票", invoiceText(order.getIsInvoice()), "状态", statusText(order.getSettleStatus()));
        summaryRow(sheet, 4, "应收合计", text(order.getTotalAmount()), "已结清", text(order.getReceivedAmount()));
        summaryRow(sheet, 5, "现金实收", text(order.getCashReceivedAmount()), "废纸抵扣", text(order.getScrapOffsetAmount()));
        summaryRow(sheet, 6, "未收金额", text(order.getUnreceivedAmount()), "备注", order.getRemark());
        summaryRow(sheet, 7, "口径说明", "应收不因废纸抵扣减少，废纸抵扣只作为结清组成。", "", "");
    }

    private static void writeHeader(Sheet sheet, CellStyle style) {
        Row row = sheet.createRow(9);
        String[] labels = {
                "原纸", "品名/规格", "原纸重量", "加工项目",
                "计费依据", "成品结果", "切边", "加工费"
        };
        for (int i = 0; i < labels.length; i++) {
            row.createCell(i).setCellValue(labels[i]);
            row.getCell(i).setCellStyle(style);
        }
    }

    private static void writeLines(Sheet sheet, List<SettlePrintLineVO> lines, CellStyle style) {
        if (lines == null || lines.isEmpty()) {
            sheet.createRow(10).createCell(0).setCellValue("暂无结算明细");
            return;
        }
        int rowIndex = 10;
        String currentOrderKey = null;
        SettleExportSubtotal subtotal = new SettleExportSubtotal();
        CellStyle detailStyle = SettleSheetStyles.wrapStyle(sheet.getWorkbook());
        for (SettlePrintLineVO line : lines) {
            String orderKey = orderKey(line);
            if (!orderKey.equals(currentOrderKey)) {
                rowIndex = flushSubtotal(sheet, rowIndex, currentOrderKey, subtotal, style);
                subtotal = new SettleExportSubtotal();
                writeOrderHeader(sheet.createRow(rowIndex++), line, style);
                currentOrderKey = orderKey;
            }
            writeLine(sheet.createRow(rowIndex++), line, detailStyle);
            subtotal.add(line);
        }
        flushSubtotal(sheet, rowIndex, currentOrderKey, subtotal, style);
    }

    private static int flushSubtotal(Sheet sheet, int rowIndex, String orderKey,
                                     SettleExportSubtotal subtotal, CellStyle style) {
        if (orderKey == null) {
            return rowIndex;
        }
        writeSubtotalRow(sheet.createRow(rowIndex++), subtotal, style);
        writeFeeSummaryRow(sheet.createRow(rowIndex++), subtotal, style);
        return rowIndex;
    }

    private static void writeOrderHeader(Row row, SettlePrintLineVO line, CellStyle style) {
        row.createCell(0).setCellValue("加工单");
        row.createCell(1).setCellValue(text(line.getOrderNo()));
        row.createCell(2).setCellValue("日期");
        row.createCell(3).setCellValue(text(line.getOrderDate()));
        applyStyle(row, style);
    }

    private static void writeLine(Row row, SettlePrintLineVO line, CellStyle style) {
        row.createCell(0).setCellValue(originalText(line));
        row.createCell(1).setCellValue(originalSpec(line));
        row.createCell(2).setCellValue(weightText(line.getOriginalWeight()));
        row.createCell(3).setCellValue(processNames(line));
        row.createCell(4).setCellValue(feeBasis(line));
        row.createCell(5).setCellValue(finishResult(line));
        row.createCell(6).setCellValue(trimText(line));
        row.createCell(7).setCellValue(text(line.getProcessAmount()));
        applyStyle(row, style);
    }

    private static void writeSubtotalRow(Row row, SettleExportSubtotal subtotal, CellStyle style) {
        row.createCell(0).setCellValue(text(subtotal.orderNo) + " 小计");
        row.createCell(2).setCellValue(weightText(subtotal.originalWeight));
        row.createCell(5).setCellValue(subtotal.finishCount + " 卷 / " + weightText(subtotal.finishWeight));
        row.createCell(6).setCellValue(weightText(subtotal.trimWeight));
        row.createCell(7).setCellValue(text(subtotal.processAmount));
        applyStyle(row, style);
    }

    private static void writeFeeSummaryRow(Row row, SettleExportSubtotal subtotal, CellStyle style) {
        row.createCell(0).setCellValue("费用汇总");
        row.createCell(1).setCellValue("加工费");
        row.createCell(2).setCellValue(text(subtotal.processAmount));
        row.createCell(3).setCellValue("额外费");
        row.createCell(4).setCellValue(extraFeeText(subtotal));
        row.createCell(5).setCellValue("税费 " + text(subtotal.taxAmount));
        row.createCell(6).setCellValue("本单应收");
        row.createCell(7).setCellValue(text(subtotal.lineAmount));
        applyStyle(row, style);
    }

    private static void applyStyle(Row row, CellStyle style) {
        for (int i = 0; i < COLUMN_COUNT; i++) {
            if (row.getCell(i) == null) {
                row.createCell(i);
            }
            row.getCell(i).setCellStyle(style);
        }
    }

    private static String extraFeeText(SettleExportSubtotal subtotal) {
        String amount = text(subtotal.extraAmount);
        return subtotal.extraFeeSummary == null ? amount : amount + "（" + subtotal.extraFeeSummary + "）";
    }

    private static String orderKey(SettlePrintLineVO line) {
        return line.getOrderUuid() != null ? line.getOrderUuid() : text(line.getOrderNo());
    }

    private static void summaryRow(Sheet sheet, int rowIndex, String k1, String v1, String k2, String v2) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(k1);
        row.createCell(1).setCellValue(text(v1));
        row.createCell(3).setCellValue(k2);
        row.createCell(4).setCellValue(text(v2));
    }

    private static String periodText(SettleOrder order) {
        if (order.getPeriodStart() == null || order.getPeriodEnd() == null) {
            return "-";
        }
        return order.getPeriodStart() + " ~ " + order.getPeriodEnd();
    }

    private static String invoiceText(Integer value) {
        return value == null ? "-" : value == 1 ? "开票" : "不开票";
    }

    private static String statusText(Integer value) {
        if (value == null) {
            return "-";
        }
        return switch (value) {
            case 1 -> "待收款";
            case 2 -> "部分收款";
            case 3 -> "已结清";
            default -> String.valueOf(value);
        };
    }

    private static void autosize(Sheet sheet) {
        for (int i = 0; i < COLUMN_COUNT; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 16000));
        }
    }

}
