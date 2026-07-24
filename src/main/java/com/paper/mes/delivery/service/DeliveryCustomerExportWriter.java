package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryCustomerRevisionPreviewVO;
import com.paper.mes.delivery.dto.DeliveryCustomerSpecVO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;

final class DeliveryCustomerExportWriter {

    private static final int COLUMN_COUNT = 15;

    private DeliveryCustomerExportWriter() {
    }

    static void write(Sheet sheet, DeliveryDetailVO detail,
                      DeliveryCustomerRevisionPreviewVO customerSpecs) {
        writeSummary(sheet, detail, customerSpecs);
        writeHeader(sheet);
        int rowIndex = 8;
        for (DeliveryCustomerSpecVO item : customerSpecs.getItems()) {
            writeItem(sheet.createRow(rowIndex), rowIndex++ - 7, item);
        }
        autoSize(sheet);
    }

    private static void writeSummary(Sheet sheet, DeliveryDetailVO detail,
                                     DeliveryCustomerRevisionPreviewVO customerSpecs) {
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("出库客户单据口径");
        title.getCell(0).setCellStyle(titleStyle(sheet));
        summaryRow(sheet, 1, new Object[]{"出库单号", detail.getOrder().getDeliveryNo(),
                "客户", detail.getOrder().getCustomerName()});
        summaryRow(sheet, 2, new Object[]{"客户版本", "V" + customerSpecs.getCurrentRevisionNo(),
                "状态", revisionStatus(customerSpecs)});
        summaryRow(sheet, 3, new Object[]{"实物出库重量kg", customerSpecs.getPhysicalTotalWeight(),
                "客户单据重量kg", customerSpecs.getCustomerTotalWeight()});
        summaryRow(sheet, 4, new Object[]{"重量差异kg", customerSpecs.getDifferenceWeight(),
                "用途", "仅用于客户单据展示，不影响库存与加工费结算"});
    }

    private static void summaryRow(Sheet sheet, int rowIndex, Object[] values) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(text(values[0]));
        row.createCell(1).setCellValue(text(values[1]));
        row.createCell(3).setCellValue(text(values[2]));
        row.createCell(4).setCellValue(text(values[3]));
    }

    private static void writeHeader(Sheet sheet) {
        String[] labels = {"序号", "加工单号", "成品卷号", "实物品名", "实物克重", "实物门幅",
                "实物出库重量kg", "客户品名", "客户克重", "客户门幅", "客户单据重量kg",
                "重量差异kg", "口径状态", "值来源", "客户备注"};
        Row row = sheet.createRow(7);
        writeCells(row, labels);
        CellStyle style = headerStyle(sheet);
        for (int index = 0; index < labels.length; index++) row.getCell(index).setCellStyle(style);
        sheet.createFreezePane(0, 8);
    }

    private static void writeItem(Row row, int rowNumber, DeliveryCustomerSpecVO item) {
        BigDecimal difference = difference(item.getCustomerDisplayWeight(), item.getPhysicalDeliveryWeight());
        String status = item.isSpecificationChanged() || item.isWeightChanged() ? "已调整" : "与实物一致";
        Object[] values = {rowNumber, item.getOrderNo(), item.getFinishRollNo(), item.getPhysicalPaperName(),
                item.getPhysicalGramWeight(), item.getPhysicalFinishWidth(), item.getPhysicalDeliveryWeight(),
                item.getCustomerPaperName(), item.getCustomerGramWeight(), item.getCustomerFinishWidth(),
                item.getCustomerDisplayWeight(), difference, status, sourceText(item.getValueSource()),
                item.getCustomerRemark()};
        writeCells(row, values);
    }

    private static BigDecimal difference(BigDecimal customer, BigDecimal physical) {
        if (customer == null || physical == null) return null;
        return customer.subtract(physical).stripTrailingZeros();
    }

    private static String sourceText(String value) {
        if ("DELIVERY_REVISION".equals(value)) return "出库客户更正版";
        if ("HISTORICAL_BASELINE".equals(value)) return "历史出库实物基线";
        if ("FINISH_DEFAULT".equals(value)) return "加工成品客户口径";
        if ("PHYSICAL".equals(value)) return "实物口径";
        return value;
    }

    private static String revisionStatus(DeliveryCustomerRevisionPreviewVO specs) {
        String kind = specs.getCurrentRevisionKind();
        if (DeliveryCustomerRevisionPreviewService.REVISION_KIND_SYSTEM.equals(kind)) return "出库确认冻结基线";
        if (DeliveryCustomerRevisionPreviewService.REVISION_KIND_USER.equals(kind)) return "已发布客户更正版";
        if (DeliveryCustomerRevisionPreviewService.REVISION_KIND_HISTORICAL.equals(kind)) return "历史出库实物基线";
        return "继承加工成品口径";
    }

    private static void writeCells(Row row, Object[] values) {
        for (int index = 0; index < values.length; index++) {
            row.createCell(index).setCellValue(text(values[index]));
        }
    }

    private static CellStyle titleStyle(Sheet sheet) {
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private static CellStyle headerStyle(Sheet sheet) {
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static void autoSize(Sheet sheet) {
        for (int index = 0; index < COLUMN_COUNT; index++) {
            sheet.autoSizeColumn(index);
            sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 512, 12000));
        }
    }

    private static String text(Object value) {
        if (value == null) return "-";
        if (value instanceof BigDecimal decimal) return decimal.stripTrailingZeros().toPlainString();
        return value.toString();
    }
}
