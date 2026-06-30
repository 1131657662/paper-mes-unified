package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettleDetailVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import com.paper.mes.settle.entity.SettleOrder;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算单 Excel 导出装配。
 */
@Service
public class SettleExportService {

    public Workbook buildWorkbook(SettleDetailVO detail) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("结算单");
        writeSummary(sheet, detail.getOrder(), titleStyle(workbook));
        writeHeader(sheet, headerStyle(workbook));
        writeLines(sheet, detail, subtotalStyle(workbook));
        autosize(sheet, 23);
        return workbook;
    }

    private void writeSummary(Sheet sheet, SettleOrder order, CellStyle titleStyle) {
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("客户结算单");
        title.getCell(0).setCellStyle(titleStyle);
        row(sheet, 1, "结算单号", order.getSettleNo(), "客户", order.getCustomerName());
        row(sheet, 2, "结算日期", text(order.getSettleDate()), "账期", periodText(order));
        row(sheet, 3, "开票", invoiceText(order.getIsInvoice()), "状态", statusText(order.getSettleStatus()));
        row(sheet, 4, "应收合计", text(order.getTotalAmount()), "未收金额", text(order.getUnreceivedAmount()));
        row(sheet, 5, "备注", order.getRemark(), "", "");
    }

    private void writeHeader(Sheet sheet, CellStyle style) {
        Row row = sheet.createRow(7);
        String[] labels = {
                "序号", "加工单", "日期", "原纸", "品名", "克重", "门幅", "原纸重量kg",
                "加工内容", "成品摘要", "成品数", "成品重量kg", "切边kg", "锯纸单价",
                "锯纸费", "复卷单价", "复卷费", "加工费", "额外费", "额外费说明", "开票加价", "开票", "应收合计"
        };
        for (int i = 0; i < labels.length; i++) {
            row.createCell(i).setCellValue(labels[i]);
            row.getCell(i).setCellStyle(style);
        }
    }

    private void writeLines(Sheet sheet, SettleDetailVO detail, CellStyle subtotalStyle) {
        int rowIndex = 8;
        int index = 1;
        List<SettlePrintLineVO> lines = detail.getPrintLines();
        String currentOrderKey = null;
        OrderSubtotal subtotal = new OrderSubtotal();
        for (SettlePrintLineVO line : lines) {
            String orderKey = orderKey(line);
            if (currentOrderKey != null && !currentOrderKey.equals(orderKey)) {
                rowIndex = writeSubtotalRow(sheet, rowIndex, subtotal, subtotalStyle);
                subtotal = new OrderSubtotal();
            }
            currentOrderKey = orderKey;
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(index++);
            row.createCell(1).setCellValue(text(line.getOrderNo()));
            row.createCell(2).setCellValue(text(line.getOrderDate()));
            row.createCell(3).setCellValue(text(line.getOriginalLabel()));
            row.createCell(4).setCellValue(text(line.getPaperName()));
            row.createCell(5).setCellValue(text(line.getGramWeight()));
            row.createCell(6).setCellValue(text(line.getOriginalWidth()));
            row.createCell(7).setCellValue(text(line.getOriginalWeight()));
            row.createCell(8).setCellValue(text(line.getProcessText()));
            row.createCell(9).setCellValue(text(line.getFinishSummary()));
            row.createCell(10).setCellValue(text(line.getFinishCount()));
            row.createCell(11).setCellValue(text(line.getFinishWeight()));
            row.createCell(12).setCellValue(text(line.getTrimWeight()));
            row.createCell(13).setCellValue(unitPriceText(line.getSawUnitPrice(), line.getSawInvoiceUnitPrice()));
            row.createCell(14).setCellValue(text(line.getSawAmount()));
            row.createCell(15).setCellValue(unitPriceText(line.getRewindUnitPrice(), line.getRewindInvoiceUnitPrice()));
            row.createCell(16).setCellValue(text(line.getRewindAmount()));
            row.createCell(17).setCellValue(text(line.getProcessAmount()));
            row.createCell(18).setCellValue(text(line.getExtraAmount()));
            row.createCell(19).setCellValue(text(line.getExtraFeeSummary()));
            row.createCell(20).setCellValue(text(line.getTaxAmount()));
            row.createCell(21).setCellValue(invoiceText(line.getIsInvoice()));
            row.createCell(22).setCellValue(text(line.getLineAmount()));
            subtotal.add(line);
        }
        if (currentOrderKey != null) {
            writeSubtotalRow(sheet, rowIndex, subtotal, subtotalStyle);
        }
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

    private CellStyle subtotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private int writeSubtotalRow(Sheet sheet, int rowIndex, OrderSubtotal subtotal, CellStyle style) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(1).setCellValue(text(subtotal.orderNo) + " 小计");
        row.createCell(7).setCellValue(text(subtotal.originalWeight));
        row.createCell(10).setCellValue(text(subtotal.finishCount));
        row.createCell(11).setCellValue(text(subtotal.finishWeight));
        row.createCell(12).setCellValue(text(subtotal.trimWeight));
        row.createCell(17).setCellValue(text(subtotal.processAmount));
        row.createCell(18).setCellValue(text(subtotal.extraAmount));
        row.createCell(20).setCellValue(text(subtotal.taxAmount));
        row.createCell(22).setCellValue(text(subtotal.lineAmount));
        for (int i = 0; i <= 22; i++) {
            if (row.getCell(i) == null) {
                row.createCell(i);
            }
            row.getCell(i).setCellStyle(style);
        }
        return rowIndex;
    }

    private String orderKey(SettlePrintLineVO line) {
        if (line.getOrderUuid() != null) {
            return line.getOrderUuid();
        }
        return line.getOrderNo() == null ? "" : line.getOrderNo();
    }

    private void row(Sheet sheet, int rowIndex, String k1, String v1, String k2, String v2) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(k1);
        row.createCell(1).setCellValue(text(v1));
        row.createCell(3).setCellValue(k2);
        row.createCell(4).setCellValue(text(v2));
    }

    private String periodText(SettleOrder order) {
        if (order.getPeriodStart() == null || order.getPeriodEnd() == null) {
            return "-";
        }
        return order.getPeriodStart() + " ~ " + order.getPeriodEnd();
    }

    private String invoiceText(Integer value) {
        if (value == null) {
            return "-";
        }
        return value == 1 ? "开票" : "不开票";
    }

    private String statusText(Integer value) {
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

    private void autosize(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 12000));
        }
    }

    private String text(Object value) {
        return value == null ? "-" : value.toString();
    }

    private String text(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private String unitPriceText(BigDecimal price, BigDecimal invoicePrice) {
        if (price == null) {
            return "-";
        }
        if (invoicePrice == null || invoicePrice.compareTo(price) == 0) {
            return text(price);
        }
        return text(price) + "（开票价 " + text(invoicePrice) + "）";
    }

    private static class OrderSubtotal {
        private String orderNo;
        private int finishCount;
        private BigDecimal originalWeight = BigDecimal.ZERO;
        private BigDecimal finishWeight = BigDecimal.ZERO;
        private BigDecimal trimWeight = BigDecimal.ZERO;
        private BigDecimal processAmount = BigDecimal.ZERO;
        private BigDecimal extraAmount = BigDecimal.ZERO;
        private BigDecimal taxAmount = BigDecimal.ZERO;
        private BigDecimal lineAmount = BigDecimal.ZERO;

        private void add(SettlePrintLineVO line) {
            orderNo = line.getOrderNo();
            finishCount += line.getFinishCount() == null ? 0 : line.getFinishCount();
            originalWeight = originalWeight.add(nz(line.getOriginalWeight()));
            finishWeight = finishWeight.add(nz(line.getFinishWeight()));
            trimWeight = trimWeight.add(nz(line.getTrimWeight()));
            processAmount = processAmount.add(nz(line.getProcessAmount()));
            extraAmount = extraAmount.add(nz(line.getExtraAmount()));
            taxAmount = taxAmount.add(nz(line.getTaxAmount()));
            lineAmount = lineAmount.add(nz(line.getLineAmount()));
        }

        private BigDecimal nz(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }
}
